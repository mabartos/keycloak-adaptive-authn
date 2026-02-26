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
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static io.github.mabartos.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static io.github.mabartos.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

/**
 * Default risk engine for the overall risk score evaluation leveraging asynchronous and parallel processing
 */
public class DefaultRiskEngine extends AbstractRiskEngine {
    private final ExecutorsProvider executorsProvider;

    public DefaultRiskEngine(KeycloakSession session) {
        super(session);
        this.executorsProvider = session.getProvider(ExecutorsProvider.class);
    }

    @Override
    protected ResultRisk evaluateRiskContinuous(@Nonnull RealmModel realm, @Nonnull UserModel knownUser) {
        var evaluators = getRiskEvaluators(RiskEvaluator.EvaluationPhase.CONTINUOUS, realm);

        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), _ ->
                tracingProvider.trace(DefaultRiskEngine.class, "evaluateContinuous", span -> {
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
                }), "DefaultRiskEngine.handleContinuous");
    }

    @Override
    protected ResultRisk evaluateRiskBeforeAuthn(@Nonnull RealmModel realm) {
        return evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, realm, null);
    }

    @Override
    protected ResultRisk evaluateRiskUserKnown(@Nonnull RealmModel realm, @Nonnull UserModel knownUser) {
        return evaluateRiskAuthentication(RiskEvaluator.EvaluationPhase.USER_KNOWN, realm, knownUser);
    }

    protected ResultRisk evaluateRiskAuthentication(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        var exec = executorsProvider.getExecutor("risk-engine");

        var timeout = getNumberRealmAttribute(realm, EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(realm, EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        // Init local user contexts
        userContexts.stream()
                .filter(f -> f.requiresUser() == phase.requiresKnownUser)
                .filter(f -> !f.isInitialized())
                .filter(f -> !f.isRemote())
                .forEach(f -> f.initData(realm, knownUser));

        var results = new EvaluatorResults();

        var evaluators = Multi.createFrom()
                .items(getRiskEvaluators(phase, realm))
                .onItem()
                .transformToIterable(f -> f)
                .collect()
                .asSet();

        // Evaluate factors with no user in a different worker thread, otherwise in the main thread
        evaluators = phase.requiresKnownUser ? evaluators : evaluators.runSubscriptionOn(exec);

        evaluators.subscribe().with(e -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session.getContext(), s -> {
            tracingProvider.trace(DefaultRiskEngine.class, "evaluateAll", span -> {
                var evaluatedRisks = Multi.createFrom()
                        .items(e.stream())
                        .onItem().transformToUniAndConcatenate(risk -> processEvaluator(risk, realm, knownUser, phase.requiresKnownUser, exec, retries, timeout, results))
                        .filter(Objects::nonNull)
                        .collect()
                        .asSet();

                evaluatedRisks.subscribe().with(risks -> {
                    this.risk = riskScoreAlgorithm.evaluateRisk(risks, phase, realm, knownUser);

                    if (risk.isValid()) {
                        logger.debugf("The overall risk score is %f - (evaluation phase: %s)", risk.getScore(), phase);

                        if (span.isRecording()) {
                            span.setAttribute("keycloak.risk.engine.overall", risk.getScore());
                            span.setAttribute("keycloak.risk.engine.phase", phase.name());
                        }

                        storedRiskProvider.storeRisk(risk, phase);
                    }

                    results.logAll();
                });
            });
        }), failure -> logger.error(failure.getCause()));
        //probably not a good option
        return risk;
    }

    protected Uni<RiskEvaluator> processEvaluator(RiskEvaluator evaluator, @Nonnull RealmModel realm, @Nullable UserModel knownUser, boolean requiresUser, ExecutorService thread, int retries, Duration timeout, EvaluatorResults results) {
        var item = Uni.createFrom()
                .item(evaluator)
                .onItem()
                .invoke(eval -> tracingProvider.trace(eval.getClass(), "evaluate", span -> {
                    executeEvaluator(eval, realm, knownUser, retries, results);

                    if (span.isRecording()) {
                        span.setAttribute("keycloak.risk.engine.evaluator.score", eval.getRisk().getScore().name());
                        eval.getRisk().getReason().ifPresent(reason -> span.setAttribute("keycloak.risk.engine.evaluator.reason", reason));
                        span.setAttribute("keycloak.risk.engine.evaluator.weight", eval.getWeight(realm));
                    }
                }))
                .onFailure()
                .recoverWithUni(Uni.createFrom().nothing())
                .ifNoItem()
                .after(timeout)
                .recoverWithUni(Uni.createFrom().nothing());
        return requiresUser ? item : item.emitOn(thread);
    }
}