/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.engine;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.RiskScoreAlgorithm;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

/**
 * Default risk engine for the overall risk score evaluation leveraging asynchronous and parallel processing
 */
public class DefaultRiskEngine implements RiskEngine {
    // TODO have it configurable
    protected static final double RISK_THRESHOLD_LOG_OUT_USER = 0.8;
    private static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final TracingProvider tracingProvider;
    private final RiskScoreAlgorithm riskScoreAlgorithm;
    private final Set<RiskEvaluator> riskEvaluators;
    private final Set<UserContext> userContexts;
    private final ExecutorsProvider executorsProvider;
    private final StoredRiskProvider storedRiskProvider;

    private Risk risk = Risk.invalid();

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskScoreAlgorithm = session.getProvider(RiskScoreAlgorithm.class);
        this.riskEvaluators = session.getAllProviders(RiskEvaluator.class);
        this.userContexts = session.getAllProviders(UserContext.class);
        this.executorsProvider = session.getProvider(ExecutorsProvider.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public void evaluateRisk(RiskEvaluator.EvaluationPhase phase) {
        logger.debugf("Risk Engine - EVALUATING (phase: %s)", phase.name());
        var start = Time.currentTimeMillis();

        switch (phase) {
            case CONTINUOUS -> handleContinuous();
            case BEFORE_AUTHN, USER_KNOWN -> handleAuthentication(phase);
        }

        logger.debugf("Risk Engine - STOPPED EVALUATING (phase: %s) - consumed time: '%d ms'", phase.name(), Time.currentTimeMillis() - start);
    }

    protected void handleContinuous() {
        var evaluators = getRiskEvaluators(RiskEvaluator.EvaluationPhase.CONTINUOUS);
        evaluators.forEach(RiskEvaluator::evaluateRisk);
        var risk = riskScoreAlgorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS);

        if (risk.isValid() && risk.getScore().get() >= RISK_THRESHOLD_LOG_OUT_USER) {
            // TODO log out user - remove all user's userSessions
        }
    }

    protected void handleAuthentication(RiskEvaluator.EvaluationPhase phase) {
        // It is not necessary to evaluate the risk multiple times for 'BEFORE_AUTHN' phase
        if (phase == RiskEvaluator.EvaluationPhase.BEFORE_AUTHN) {
            final var storedRisk = storedRiskProvider.getStoredRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
            if (storedRisk.isValid()) {
                logger.debugf("Risk for the phase 'NO_USER' was already evaluated (score: %f). Skipping the evaluation", storedRisk.getScore().get());
                this.risk = storedRisk;
                return;
            }
        }

        var requiresUser = phase == RiskEvaluator.EvaluationPhase.USER_KNOWN;
        var exec = executorsProvider.getExecutor("risk-engine");

        var timeout = getNumberRealmAttribute(EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        // Init blocking user contexts
        userContexts.stream()
                .filter(f -> f.requiresUser() == requiresUser)
                .filter(f -> !f.isInitialized())
                .filter(UserContext::isBlocking)
                .forEach(UserContext::initData);

        var evaluators = Multi.createFrom()
                .items(getRiskEvaluators(phase))
                .onItem()
                .transformToIterable(f -> f)
                .collect()
                .asSet();

        // Evaluate factors with no user in a different worker thread, otherwise in the main thread
        evaluators = requiresUser ? evaluators : evaluators.runSubscriptionOn(exec);

        evaluators.subscribe().with(e -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            tracingProvider.trace(DefaultRiskEngine.class, "evaluateAll", span -> {
                var evaluatedRisks = Multi.createFrom()
                        .items(e.stream())
                        .onItem().transformToUniAndConcatenate(risk -> processEvaluator(risk, requiresUser, exec, retries, timeout))
                        .filter(Objects::nonNull)
                        .collect()
                        .asSet();

                evaluatedRisks.subscribe().with(risks -> {
                    this.risk = riskScoreAlgorithm.evaluateRisk(risks, phase);

                    if (risk.isValid()) {
                        logger.debugf("The overall risk score is %f - (evaluation phase: %s)", risk.getScore().get(), phase);

                        if (span.isRecording()) {
                            span.setAttribute("keycloak.risk.engine.overall", risk.getScore().get());
                            span.setAttribute("keycloak.risk.engine.phase", phase.name());
                        }

                        storedRiskProvider.storeRisk(risk, phase);
                    }

                });
            });
        }), failure -> logger.error(failure.getCause()));
    }

    protected Uni<RiskEvaluator> processEvaluator(RiskEvaluator evaluator, boolean requiresUser, ExecutorService thread, int retries, Duration timeout) {
        var item = Uni.createFrom()
                .item(evaluator)
                .onItem()
                .invoke(eval -> tracingProvider.trace(eval.getClass(), "evaluate", span -> {
                    for (int i = 0; i < retries; i++) {
                        eval.evaluateRisk();
                        if (eval.getRisk() != Risk.invalid()) {
                            break;
                        }
                    }

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.evaluator.score", eval.getRisk().getScore().orElse(-1.0));
                        eval.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                        span.setAttribute("keycloak.risk.engine.evaluator.weight", eval.getWeight());
                    }
                }))
                .onFailure()
                .recoverWithUni(Uni.createFrom().nothing())
                .ifNoItem()
                .after(timeout)
                .recoverWithUni(Uni.createFrom().nothing());
        return requiresUser ? item : item.emitOn(thread);
    }

    @Override
    public Risk getOverallRisk() {
        return risk;
    }

    @Override
    public Risk getRisk(RiskEvaluator.EvaluationPhase phase) {
        if (phase == null) {
            return Risk.invalid();
        }
        return storedRiskProvider.getStoredRisk(phase);
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators(RiskEvaluator.EvaluationPhase phase) {
        return riskEvaluators.stream()
                .filter(f -> f.evaluationPhases().contains(phase))
                .filter(RiskEvaluator::isEnabled)
                .collect(Collectors.toSet());
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}