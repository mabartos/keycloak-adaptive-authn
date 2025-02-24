package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.tracing.TracingProviderUtil;

import java.util.Set;
import java.util.stream.Collectors;

public class SynchronousRiskEngine implements RiskEngine {

    private static final Logger logger = Logger.getLogger(SynchronousRiskEngine.class);

    private final TracingProvider tracingProvider;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private final StoredRiskProvider storedRiskProvider;

    private Double risk;

    public SynchronousRiskEngine(KeycloakSession session) {
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
        return riskFactorEvaluators.stream().filter(f -> f.requiresUser() == requiresUser).filter(RiskEvaluator::isEnabled).collect(Collectors.toSet());
    }

    @Override
    public void evaluateRisk(boolean requiresUser) {
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
        var riskPhase = requiresUser ? StoredRiskProvider.RiskPhase.REQUIRES_USER : StoredRiskProvider.RiskPhase.NO_USER;

        tracingProvider.trace(SynchronousRiskEngine.class, "evaluateAll", span -> {
            getRiskEvaluators(requiresUser).forEach(RiskEvaluator::evaluateRisk);

            var weightedRisk = getRiskEvaluators(requiresUser).stream()
                    .filter(eval -> eval.getRiskScore().isPresent())
                    .peek(eval -> logger.debugf("Evaluator: %s", eval.getClass().getSimpleName()))
                    .peek(eval -> logger.debugf("Risk evaluated: %f (weight %f)", eval.getRiskScore().get(), eval.getWeight()))
                    .mapToDouble(eval -> eval.getRiskScore().get() * eval.getWeight())
                    .sum();

            var weights = getRiskEvaluators(requiresUser).stream()
                    .mapToDouble(RiskEvaluator::getWeight)
                    .sum();

            this.risk = weightedRisk / weights;
            logger.debugf("SYNC - the overall risk score is %f - (requires user: %s)", risk, requiresUser);

            storedRiskProvider.storeRisk(risk, riskPhase);
        });
    }
}
