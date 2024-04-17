package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.browser.BrowserRiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(DefaultRiskEngine.class);

    private final KeycloakSession session;
    private final Set<RiskEvaluator> riskFactorEvaluators;
    private Double risk;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.riskFactorEvaluators = session.getAllProviders(RiskEvaluator.class);
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");

        getRiskEvaluators().forEach(RiskEvaluator::evaluate);

        var filteredEvaluators = getRiskEvaluators().stream()
                .filter(f -> isValidValue(f.getWeight()))
                .filter(f -> isValidValue(f.getRiskValue()))
                .toList();

        var weightedRisk = filteredEvaluators.stream()
                .peek(f -> logger.debugf("Evaluator: %s", f.getClass().getSimpleName()))
                .peek(f -> logger.debugf("Risk evaluated: %f (weight %f)", f.getRiskValue(), f.getWeight()))
                .mapToDouble(f -> f.getRiskValue() * f.getWeight())
                .sum();
        var weights = filteredEvaluators
                .stream()
                .mapToDouble(RiskEvaluator::getWeight)
                .sum();

        // Weighted mean
        this.risk = weightedRisk / weights;
        logger.debugf("The overall risk score is %f", risk);
    }

    @Override
    public Double getRisk() {
        return risk;
    }

    @Override
    public Set<UserContext<?>> getRiskFactors() {
        return riskFactorEvaluators.stream()
                .flatMap(f -> f.getUserContexts().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RiskEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        evaluateRisk();
        storeRisk(context);
        context.success();
    }

    private boolean isValidValue(Double value) {
        if (value == null) return false;
        return value >= 0.0d && value <= 1.0d;
    }
}