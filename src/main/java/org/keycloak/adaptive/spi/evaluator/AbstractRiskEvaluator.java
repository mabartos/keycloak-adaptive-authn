package org.keycloak.adaptive.spi.evaluator;

import java.util.Optional;

public abstract class AbstractRiskEvaluator implements RiskEvaluator {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Double> riskScore;

    @Override
    public Optional<Double> getRiskScore() {
        return riskScore;
    }

    @Override
    public abstract boolean requiresUser();

    public abstract double getWeight();

    public abstract Optional<Double> evaluate();

    @Override
    public void evaluateRisk() {
        this.riskScore = evaluate();
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void close() {

    }
}
