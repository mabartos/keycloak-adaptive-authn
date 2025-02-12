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
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

/**
 * Default risk engine for the overall risk score evaluation leveraging asynchronous and parallel processing
 */
public class DefaultRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final TracingProvider tracingProvider;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private final ExecutorsProvider executorsProvider;
    private final StoredRiskProvider storedRiskProvider;

    private boolean requiresUser;
    private StoredRiskProvider.RiskPhase riskPhase;
    private Double risk;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
        this.executorsProvider = session.getProvider(ExecutorsProvider.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");

        // It is not necessary to evaluate the risk multiple times for 'NO_USER' phase
        if (!requiresUser) {
            final var storedRisk = storedRiskProvider.getStoredRisk(StoredRiskProvider.RiskPhase.NO_USER);
            if (storedRisk.isPresent()) {
                logger.debugf("Risk for the phase 'NO_USER' was already evaluated (score: %f). Skipping the evaluation", storedRisk.get());
                this.risk = storedRisk.get();
                return;
            }
        }

        var start = Time.currentTimeMillis();
        var exec = executorsProvider.getExecutor("risk-engine");

        var timeout = getNumberRealmAttribute(EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        var evaluators = Multi.createFrom()
                .items(getRiskEvaluators())
                .onItem()
                .transformToIterable(f -> f)
                .filter(RiskEvaluator::isEnabled)
                .filter(f -> f.requiresUser() == this.requiresUser)
                .collect()
                .asSet();

        // Evaluate factors with no user in a different worker thread, otherwise in the main thread
        evaluators = requiresUser ? evaluators : evaluators.runSubscriptionOn(exec);

        evaluators.subscribe().with(e -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            tracingProvider.trace(DefaultRiskEngine.class, "evaluateAll", span -> {
                var evaluatedRisks = Multi.createFrom()
                        .items(e.stream())
                        .onItem().transformToUniAndConcatenate(risk -> processEvaluator(risk, exec, retries))
                        .ifNoItem()
                        .after(timeout)
                        .recoverWithCompletion()
                        .filter(f -> f.getRiskValue().isPresent())
                        .filter(f -> RiskEngine.isValidValue(f.getWeight()) && RiskEngine.isValidValue(f.getRiskValue().get()))
                        .collect()
                        .asSet();

                evaluatedRisks.subscribe().with(risks -> {
                    var weightedRisk = risks.stream()
                            .filter(eval -> eval.getRiskValue().isPresent())
                            .peek(eval -> logger.debugf("Evaluator: %s", eval.getClass().getSimpleName()))
                            .peek(eval -> logger.debugf("Risk evaluated: %f (weight %f)", eval.getRiskValue().get(), eval.getWeight()))
                            .mapToDouble(eval -> eval.getRiskValue().get() * eval.getWeight())
                            .sum();

                    var weights = risks.stream()
                            .mapToDouble(RiskEvaluator::getWeight)
                            .sum();

                    // Weighted arithmetic mean
                    this.risk = weightedRisk / weights;
                    logger.debugf("The overall risk score is %f - (requires user: %s)", risk, requiresUser);

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.overall", risk);
                        span.setAttribute("keycloak.risk.engine.phase", riskPhase.name());
                    }

                    storedRiskProvider.storeRisk(risk, riskPhase);
                });
            });
        }), failure -> logger.error(failure.getCause()));
        logger.debugf("Consumed time: '%d ms'", Time.currentTimeMillis() - start);
    }

    protected Uni<RiskEvaluator> processEvaluator(RiskEvaluator evaluator, ExecutorService thread, int retries) {
        var item = Uni.createFrom()
                .item(evaluator)
                .onItem()
                .invoke(eval -> tracingProvider.trace(eval.getClass(), "evaluate", span -> {
                    eval.evaluate();

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.evaluator.score", eval.getRiskValue().orElse(-1.0));
                        span.setAttribute("keycloak.risk.engine.evaluator.weight", eval.getWeight());
                    }
                }))
                .onFailure()
                .retry()
                .atMost(retries);
        return requiresUser ? item : item.emitOn(thread);
    }

    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public boolean requiresUser(AuthenticatorConfigModel configModel) {
        return Optional.ofNullable(configModel)
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        this.requiresUser = requiresUser(context.getAuthenticatorConfig());
        this.riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;

        final var storedRisk = storedRiskProvider.getStoredRisk(riskPhase);

        if (storedRisk.isPresent()) {
            logger.debugf("Risk for phase '%s' is already evaluated ('%s'). Skipping it...", riskPhase, storedRisk.get());
        } else {
            evaluateRisk();
        }

        context.success();
    }

    protected <T extends Number> Optional<T> getNumberRealmAttribute(String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}