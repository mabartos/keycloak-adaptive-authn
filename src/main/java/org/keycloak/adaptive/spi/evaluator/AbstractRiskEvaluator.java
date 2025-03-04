package org.keycloak.adaptive.spi.evaluator;

import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

/**
 * Abstraction over the {@link RiskEvaluator} to simplify risk evaluators
 */
public abstract class AbstractRiskEvaluator implements RiskEvaluator {
    protected Risk risk = Risk.invalid();

    public abstract KeycloakSession getSession();

    @Override
    public abstract Set<EvaluationPhase> evaluationPhases();

    @Override
    public Risk getRisk() {
        this.risk = risk == null ? Risk.invalid() : risk;
        return risk;
    }

    /**
     * Default/starting weight for the evaluator
     */
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

    /**
     * Evaluate risk and return the {@link Risk} object.
     * Never returns null - return {@link Risk#invalid()} instead.
     *
     * @return risk object
     */
    public abstract Risk evaluate();

    @Override
    public boolean allowRetries() {
        return true;
    }

    @Override
    public void evaluateRisk() {
        this.risk = evaluate();
    }

    @Override
    public void close() {

    }
}
