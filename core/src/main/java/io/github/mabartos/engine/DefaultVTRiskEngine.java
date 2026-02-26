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

import io.github.mabartos.level.ResultRisk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.github.mabartos.engine.DefaultVTRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static io.github.mabartos.engine.DefaultVTRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static io.github.mabartos.engine.DefaultVTRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static io.github.mabartos.engine.DefaultVTRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

/**
 * Risk engine implementation using virtual threads for parallel evaluation with timeout support
 * <p>
 * When used, the JAVA_OPTS_APPEND=--enable-preview should be set when starting server
 */
public class DefaultVTRiskEngine extends AbstractRiskEngine {
    public DefaultVTRiskEngine(KeycloakSession session) {
        super(session);
    }

    @Override
    protected ResultRisk evaluateRiskContinuous(@Nonnull RealmModel realm, @Nonnull UserModel knownUser) {
        var evaluators = getRiskEvaluators(RiskEvaluator.EvaluationPhase.CONTINUOUS, realm);

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), _ ->
                tracingProvider.trace(DefaultVTRiskEngine.class, "evaluateContinuous", span -> {
                    var results = new EvaluatorResults();
                    evaluators.forEach(evaluator -> executeEvaluator(evaluator, realm, knownUser, 1, results));
                    var risk = riskScoreAlgorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS, realm, knownUser);

                    if (risk.isValid()) {
                        if (span.isRecording()) {
                            span.setAttribute("keycloak.risk.engine.overall", risk.getScore());
                            span.setAttribute("keycloak.risk.engine.phase", RiskEvaluator.EvaluationPhase.CONTINUOUS.name());
                        }

                        if (risk.getScore() >= RISK_THRESHOLD_LOG_OUT_USER) {
                            session.sessions().removeUserSessions(realm, knownUser);
                            logger.warnf("User with ID %s was logged out due to suspicious activity. Evaluated risk score was %f.%s",
                                    knownUser.getId(),
                                    risk.getScore(),
                                    risk.getSummary().orElse(""));
                        }
                    }
                    results.logAll();
                    return risk;
                }), "DefaultVTRiskEngine.evaluateRiskContinuous");
    }

    @Override
    protected ResultRisk evaluateRiskBeforeAuthn(@Nonnull RealmModel realm) {
        return evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, realm, null);
    }

    @Override
    protected ResultRisk evaluateRiskUserKnown(@Nonnull RealmModel realm, @Nonnull UserModel knownUser) {
        return evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.USER_KNOWN, realm, knownUser);
    }

    // propagate UserModel user
    protected ResultRisk evaluateRiskAuthentication(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        var timeout = getNumberRealmAttribute(realm, EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(realm, EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        userContexts.stream()
                .filter(f -> f.requiresUser() == phase.requiresKnownUser)
                .filter(f -> !f.isInitialized())
                .filter(f -> !f.isRemote())
                .forEach(f -> f.initData(realm, knownUser));

        var evaluators = getRiskEvaluators(phase, realm);
        var evaluatedRisks = evaluateInParallel(evaluators, realm, knownUser, retries, timeout);

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s ->
                tracingProvider.trace(DefaultVTRiskEngine.class, "evaluateAll", span -> {
                    this.risk = riskScoreAlgorithm.evaluateRisk(evaluatedRisks, phase, realm, knownUser);

                    if (risk.isValid()) {
                        logger.debugf("The overall risk score is %f - (evaluation phase: %s)", risk.getScore(), phase);

                        if (span.isRecording()) {
                            span.setAttribute("keycloak.risk.engine.overall", risk.getScore());
                            span.setAttribute("keycloak.risk.engine.phase", phase.name());
                        }

                        storedRiskProvider.storeRisk(risk, phase);
                    }
                    return risk;
                }), "DefaultVTRiskEngine.evaluateRiskAuthentication");
    }

    protected Set<RiskEvaluator> evaluateInParallel(Set<RiskEvaluator> evaluators, @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, @Nonnull Duration timeout) {
        Map<RiskEvaluator, Boolean> completedEvaluators = new ConcurrentHashMap<>();
        var results = new EvaluatorResults();

        try (var scope = new StructuredTaskScope<RiskEvaluator>()) {
            for (var evaluator : evaluators) {
                scope.fork(() -> {
                    try {
                        // Create transaction context for this virtual thread
                        return KeycloakModelUtils.runJobInTransactionWithResult(
                                session.getKeycloakSessionFactory(),
                                session.getContext(),
                                s -> {
                                    processEvaluator(evaluator, realm, knownUser, retries, results);
                                    completedEvaluators.put(evaluator, true);
                                    return evaluator;
                                }
                                , "evaluateInParallel");
                    } catch (Exception e) {
                        logger.warnf(e, "Evaluator %s failed with exception", evaluator.getClass().getSimpleName());
                        return null;
                    }
                });
            }

            try {
                scope.joinUntil(java.time.Instant.now().plus(timeout));
                logger.debugf("Risk evaluation completed - %d/%d evaluators finished in time", completedEvaluators.size(), evaluators.size());
            } catch (TimeoutException e) {
                logger.warnf("Risk evaluation timeout exceeded: %d ms - %d/%d evaluators completed",
                        timeout.toMillis(), completedEvaluators.size(), evaluators.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Risk evaluation was interrupted", e);
            }
        } catch (Exception e) {
            logger.error("Error during parallel risk evaluation", e);
        }

        results.logAll();

        return completedEvaluators.keySet().stream()
                .filter(e -> e.getRisk().isValid())
                .collect(Collectors.toSet());
    }

    protected void processEvaluator(@Nonnull RiskEvaluator evaluator, @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, EvaluatorResults results) {
        tracingProvider.trace(evaluator.getClass(), "evaluate", span -> {
            executeEvaluator(evaluator, realm, knownUser, retries, results);

            if (span.isRecording()) {
                span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                evaluator.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                span.setAttribute("keycloak.risk.engine.evaluator.weight", evaluator.getWeight(realm));
            }
        });
    }
}
