package org.keycloak.adaptive.spi.evaluator;

import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractRiskEvaluator implements RiskEvaluator {
    private Risk risk = Risk.invalid();

    public abstract KeycloakSession getSession();

    @Override
    public abstract boolean requiresUser();

    @Override
    public Risk getRisk() {
        this.risk = risk == null ? Risk.invalid() : risk;
        return risk;
    }

    public double getDefaultWeight() {
        return Weight.DEFAULT;
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(getSession(), this.getClass(), getDefaultWeight());
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(getSession(), this.getClass());
    }

    public abstract Risk evaluate();

    @Override
    public void evaluateRisk() {
        this.risk = evaluate();
    }

    @Override
    public void close() {

    }
}
