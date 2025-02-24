package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;

// WiP - Available only when JDK >= 21 is used
public class VirtualThreadsRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(VirtualThreadsRiskEngine.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final TracingProvider tracingProvider;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private final StoredRiskProvider storedRiskProvider;

    private Double risk;

    public VirtualThreadsRiskEngine(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tracingProvider = TracingProviderUtil.getTracingProvider(session);
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }


    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators(boolean requiresUser) {
        return riskFactorEvaluators.stream()
                .filter(f -> f.requiresUser() == requiresUser)
                .filter(RiskEvaluator::isEnabled)
                .collect(Collectors.toSet());
    }

    @Override
    public void evaluateRisk(boolean requiresUser) {
        logger.debugf("Risk Engine - EVALUATING");

        if (!requiresUser) {
            var storedRisk = storedRiskProvider.getStoredRisk(StoredRiskProvider.RiskPhase.NO_USER);
            if (storedRisk.isPresent()) {
                logger.debugf("Risk for the phase 'NO_USER' was already evaluated (score: %f). Skipping the evaluation", storedRisk.get());
                this.risk = storedRisk.get();
                return;
            }
        }

        var riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;
        var start = Time.currentTimeMillis();
        var timeout = getNumberRealmAttribute(EVALUATOR_TIMEOUT_CONFIG, Long::parseLong)
                .map(Duration::ofMillis)
                .orElse(DEFAULT_EVALUATOR_TIMEOUT);
        var retries = getNumberRealmAttribute(EVALUATOR_RETRIES_CONFIG, Integer::parseInt).orElse(DEFAULT_EVALUATOR_RETRIES);

        Set<RiskEvaluator> evaluators = getRiskEvaluators(requiresUser);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session.getContext(), s -> {
                var futures = evaluators.stream()
                        .map(evaluator -> CompletableFuture.supplyAsync(() -> processEvaluator(evaluator, retries), executor))
                        .toList();

                var evaluatedRisks = futures.stream()
                        .map(future -> {
                            try {
                                //return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                                return future.join(); // blocking for now
                            } catch (Exception e) {
                                logger.error("Risk evaluation failed", e);
                                return null;
                            }
                        })
                        .filter(eval -> eval != null && eval.getRiskScore().isPresent())
                        .filter(eval -> Risk.isValid(eval.getWeight()) && Risk.isValid(eval.getRiskScore().get()))
                        .collect(Collectors.toSet());

                double weightedRisk = evaluatedRisks.stream()
                        .mapToDouble(eval -> eval.getRiskScore().get() * eval.getWeight())
                        .sum();

                double weights = evaluatedRisks.stream()
                        .mapToDouble(RiskEvaluator::getWeight)
                        .sum();

                this.risk = weightedRisk / weights;
                logger.debugf("The overall risk score is %f - (requires user: %s)", risk, requiresUser);

                storedRiskProvider.storeRisk(risk, riskPhase);
                return null;
            }, true, "HERE");
        }

        logger.debugf("Consumed time: '%d ms'", Time.currentTimeMillis() - start);
    }

    private RiskEvaluator processEvaluator(RiskEvaluator evaluator, int retries) {
        tracingProvider.trace(evaluator.getClass(), "evaluate", span -> {
            for (int i = 0; i < retries; i++) {
                evaluator.evaluateRisk();
                if (evaluator.getRiskScore().isPresent()) {
                    break;
                } else {
                    logger.warnf("Next attempt to evaluate risk for '%s' evaluator", evaluator.getClass().getSimpleName());
                }
            }

            if (span.isRecording()) {
                span.setAttribute("keycloak.risk.engine.evaluator.score", evaluator.getRiskScore().orElse(-1.0));
                span.setAttribute("keycloak.risk.engine.evaluator.weight", evaluator.getWeight());
            }
        });
        return evaluator;

    }

    protected <T extends
            Number> Optional<T> getNumberRealmAttribute(String attribute, Function<String, T> func) {
        try {
            return Optional.ofNullable(realm.getAttribute(attribute)).map(func);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
