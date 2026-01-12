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
package io.github.mabartos.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.ContinuousRiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static io.github.mabartos.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG;

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
    private final Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators;
    private final Set<UserContext> userContexts;
    private final ExecutorsProvider executorsProvider;
    private final StoredRiskProvider storedRiskProvider;

    private Risk risk = Risk.invalid();

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskScoreAlgorithm = session.getProvider(RiskScoreAlgorithm.class);
        this.userContexts = session.getAllProviders(UserContext.class);
        this.executorsProvider = session.getProvider(ExecutorsProvider.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
        this.riskEvaluators = initializeRiskEvaluators(session);
    }

    private static Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> initializeRiskEvaluators(KeycloakSession session) {
        Map<RiskEvaluator.EvaluationPhase, Set<RiskEvaluator>> riskEvaluators = new EnumMap<>(RiskEvaluator.EvaluationPhase.class);
        for (RiskEvaluator.EvaluationPhase phase : RiskEvaluator.EvaluationPhase.values()) {
            riskEvaluators.put(phase, new LinkedHashSet<>());
        }

        session.getAllProviders(RiskEvaluator.class).stream()
                .filter(RiskEvaluator::isEnabled)
                .forEach(f -> f.evaluationPhases()
                        .forEach(phase -> riskEvaluators.get(phase).add(f)));

        return riskEvaluators;
    }

    @Override
    public void evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase) {
        evaluateRisk(evaluationPhase, null, null);
    }

    @Override
    public void evaluateRisk(RiskEvaluator.EvaluationPhase phase, RealmModel realm, UserModel knownUser) {
        if (!isRiskBasedAuthnEnabled()) {
            logger.warn("Risk-based authentication is disabled. Skipping risk evaluation.");
            return;
        }

        logger.debug("--------------------------------------------------");
        logger.debugf("Risk Engine - EVALUATING (username: '%s', phase: %s)", knownUser != null ? knownUser.getUsername() : "N/A", phase.name());
        var start = Time.currentTimeMillis();

        switch (phase) {
            case CONTINUOUS -> handleContinuous(realm, knownUser);
            case BEFORE_AUTHN, USER_KNOWN -> handleAuthentication(phase);
        }

        logger.debugf("Risk Engine - STOPPED EVALUATING (phase: %s) - consumed time: '%d ms'", phase.name(), Time.currentTimeMillis() - start);
        logger.debug("--------------------------------------------------");
    }

    @Override
    public boolean isRiskBasedAuthnEnabled() {
        var realm = session.getContext().getRealm();
        return Optional.ofNullable(realm.getAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(true); // Default to enabled if not configured
    }

    protected void handleContinuous(RealmModel realm, UserModel knownUser) {
        if (realm == null || knownUser == null) {
            logger.warn("Cannot execute continuous risk score evaluation, because we need to know who is the current user and what realm is used");
            return;
        }

        var evaluators = getRiskEvaluators(RiskEvaluator.EvaluationPhase.CONTINUOUS);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            tracingProvider.trace(DefaultRiskEngine.class, "evaluateContinuous", span -> {
                evaluators.forEach(evaluator -> ((ContinuousRiskEvaluator) evaluator).evaluateRisk(realm, knownUser));
                var risk = riskScoreAlgorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS);

                if (risk.isValid()) {
                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.overall", risk.getScore().get());
                        span.setAttribute("keycloak.risk.engine.phase", RiskEvaluator.EvaluationPhase.CONTINUOUS.name());
                    }

                    if (risk.getScore().get() >= RISK_THRESHOLD_LOG_OUT_USER) {
                        session.sessions().removeUserSessions(realm, knownUser);
                        logger.warnf("User with ID %s was logged out due to suspicious activity. Evaluated risk score was %f.%s",
                                knownUser.getId(),
                                risk.getScore().get(),
                                risk.getReason().orElse(""));
                    }
                }
            });
        });
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

        // Init local user contexts
        userContexts.stream()
                .filter(f -> f.requiresUser() == requiresUser)
                .filter(f -> !f.isInitialized())
                .filter(f -> !f.isRemote())
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
                    var retriesCount = eval.allowRetries() ? retries : 1;
                    for (int i = 0; i < retriesCount; i++) {
                        eval.evaluateRisk();
                        if (!eval.getRisk().isValid()) {
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
            return Risk.invalid("Invalid evaluation phase");
        }
        return storedRiskProvider.getStoredRisk(phase);
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators(RiskEvaluator.EvaluationPhase phase) {
        if (phase == null) {
            return Collections.emptySet();
        }
        return riskEvaluators.get(phase);
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}