package org.keycloak.adaptive.spi.evaluator;

import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public abstract class AbstractRiskEvaluator implements RiskEvaluator {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<Double> riskScore;

    public abstract KeycloakSession getSession();

    @Override
    public abstract boolean requiresUser();

    @Override
    public Optional<Double> getRiskScore() {
        return riskScore;
    }

    public double getDefaultWeight() {
        return Weight.NORMAL;
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(getSession(), this.getClass(), getDefaultWeight());
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(getSession(), this.getClass());
    }

    public abstract Optional<Double> evaluate();

    @Override
    public void evaluateRisk() {
        this.riskScore = evaluate();
    }

    @Override
    public void close() {

    }
}
