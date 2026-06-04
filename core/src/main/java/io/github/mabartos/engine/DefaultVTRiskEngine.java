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

import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.ResultRisk;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
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
                    var risk = getRiskScoreAlgorithm(realm).evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS, realm, knownUser);

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

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            Tracer tracer = tracingProvider.getTracer(RiskEngine.class.getSimpleName());
            SpanBuilder spanBuilder = tracer.spanBuilder("%s.evaluateAll".formatted(RiskEngine.class.getSimpleName()));
            Span span = tracingProvider.startSpan(spanBuilder);
            try {
                if (span.isRecording()) {
                    span.setAttribute("keycloak.risk.engine.provider", DefaultVTRiskEngine.class.getSimpleName());
                }

                var evaluatedRisks = evaluateInParallel(evaluators, realm, knownUser, retries, timeout, tracer, span);
                var algorithm = getRiskScoreAlgorithm(realm);
                risk = algorithm.evaluateRisk(evaluatedRisks, phase, realm, knownUser);

                if (risk.isValid()) {
                    logger.debugf("The phase risk score is %f - (evaluation phase: %s, algorithm: %s)", risk.getScore(), phase, algorithm.getClass().getSimpleName());

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.phase.score", risk.getScore());
                        span.setAttribute("keycloak.risk.engine.phase", phase.name());
                    }
                }

                if (phase == RiskEvaluator.EvaluationPhase.USER_KNOWN) {
                    var overallRisk = algorithm.getOverallRisk();
                    logger.debugf("The overall risk score is '%f' (algorithm: %s)", overallRisk.getScore(), algorithm.getClass().getSimpleName());
                    if (overallRisk.isValid()) {
                        storedRiskProvider.storeOverallRisk(overallRisk);
                    }
                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.overall", overallRisk.getScore());
                    }
                }
                return risk;
            } catch (Exception e) {
                tracingProvider.error(e);
            } finally {
                tracingProvider.endSpan();
            }
            return risk;
        }, "DefaultVTRiskEngine.evaluateRiskAuthentication");
    }

    protected Set<RiskEvaluator> evaluateInParallel(Set<RiskEvaluator> evaluators, @Nonnull RealmModel realm, @Nullable UserModel knownUser, int retries, @Nonnull Duration timeout, @Nonnull Tracer tracer, @Nonnull Span parentSpan) {
        var results = new EvaluatorResults();
        Set<RiskEvaluator> completedEvaluators = ConcurrentHashMap.newKeySet();

        var localEvaluators = evaluators.stream().filter(e -> !e.isRemote()).collect(Collectors.toSet());
        var remoteEvaluators = evaluators.stream().filter(RiskEvaluator::isRemote).collect(Collectors.toSet());

        for (var evaluator : localEvaluators) {
            SpanBuilder spanBuilder = tracer.spanBuilder("%s.evaluate".formatted(evaluator.getClass().getSimpleName()));
            spanBuilder.setParent(Context.current().with(parentSpan));
            Span span = spanBuilder.startSpan();
            try {
                executeEvaluator(evaluator, realm, knownUser, retries, results);
                completedEvaluators.add(evaluator);

                if (span.isRecording()) {
                    span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                    evaluator.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                    span.setAttribute("keycloak.risk.engine.evaluator.trust", evaluator.getTrust(realm));
                }
            } catch (Exception e) {
                logger.warnf(e, "Local evaluator %s failed with exception", evaluator.getClass().getSimpleName());
            } finally {
                span.end();
            }
        }

        if (!remoteEvaluators.isEmpty()) {
            try (var scope = new StructuredTaskScope<RiskEvaluator>()) {
                for (var evaluator : remoteEvaluators) {
                    AtomicReference<Span> span = new AtomicReference<>();
                    scope.fork(() -> {
                        try {
                            SpanBuilder spanBuilder = tracer.spanBuilder("%s.evaluate".formatted(evaluator.getClass().getSimpleName()));
                            spanBuilder.setParent(Context.current().with(parentSpan));
                            span.set(spanBuilder.startSpan());
                            return KeycloakModelUtils.runJobInTransactionWithResult(
                                    session.getKeycloakSessionFactory(),
                                    session.getContext(),
                                    s -> {
                                        var freshRealm = s.realms().getRealm(realm.getId());
                                        var freshUser = knownUser != null ? s.users().getUserById(freshRealm, knownUser.getId()) : null;

                                        executeEvaluator(evaluator, freshRealm, freshUser, retries, results);
                                        completedEvaluators.add(evaluator);

                                        Span currentSpan = span.get();
                                        if (currentSpan.isRecording()) {
                                            currentSpan.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRisk().getScore().name());
                                            evaluator.getRisk().getReason().ifPresent(reason -> currentSpan.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                                            currentSpan.setAttribute("keycloak.risk.engine.evaluator.trust", evaluator.getTrust(freshRealm));
                                        }
                                        return evaluator;
                                    }, "evaluateInParallel");
                        } catch (Exception e) {
                            logger.warnf(e, "Remote evaluator %s failed with exception", evaluator.getClass().getSimpleName());
                            return null;
                        } finally {
                            var currentSpan = span.get();
                            if (currentSpan != null) {
                                currentSpan.end();
                            }
                        }
                    });
                }

                try {
                    scope.joinUntil(Instant.now().plus(timeout));
                    logger.debugf("Remote risk evaluation completed - %d/%d remote evaluators finished in time",
                            completedEvaluators.size() - localEvaluators.size(), remoteEvaluators.size());
                } catch (TimeoutException e) {
                    logger.warnf("Remote risk evaluation timeout exceeded: %d ms - %d/%d remote evaluators completed",
                            timeout.toMillis(), completedEvaluators.size() - localEvaluators.size(), remoteEvaluators.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Risk evaluation was interrupted", e);
                }
            } catch (Exception e) {
                logger.error("Error during parallel remote risk evaluation", e);
            }
        }

        logger.debugf("Risk evaluation completed - %d/%d evaluators total (%d local, %d remote)",
                completedEvaluators.size(), evaluators.size(), localEvaluators.size(), remoteEvaluators.size());
        results.logAll();

        return completedEvaluators.stream()
                .filter(e -> e.getRisk().isValid())
                .collect(Collectors.toSet());
    }
}
