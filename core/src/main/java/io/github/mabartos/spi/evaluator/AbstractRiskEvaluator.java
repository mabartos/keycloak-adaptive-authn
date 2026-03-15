package io.github.mabartos.spi.evaluator;

import io.github.mabartos.evaluator.EvaluatorUtils;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.level.Trust;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

/**
 * Abstraction over the {@link RiskEvaluator} to simplify risk evaluators
 */
public abstract class AbstractRiskEvaluator implements RiskEvaluator {
    protected Risk risk = Risk.invalid("Abstract risk evaluator");

    @Override
    public abstract Set<EvaluationPhase> evaluationPhases();

    @Override
    public Risk getRisk() {
        this.risk = risk == null ? Risk.invalid("Abstract risk evaluator") : risk;
        return risk;
    }

    /**
     * Default/starting trust level for the evaluator
     */
    public double getDefaultTrust() {
        return Trust.FULL;
    }

    @Override
    public double getTrust(@Nonnull RealmModel realm) {
        return EvaluatorUtils.getStoredEvaluatorTrust(realm, this.getClass(), getDefaultTrust());
    }

    @Override
    public boolean isEnabled(@Nonnull RealmModel realm) {
        return EvaluatorUtils.isEvaluatorEnabled(realm, this.getClass());
    }

    /**
     * Evaluate risk and return the {@link Risk} object.
     * Never returns null - return {@link Risk#invalid(String)} ()} instead.
     *
     * @return risk object
     */
    public Risk evaluate(@Nonnull RealmModel realm) {
        return evaluate(realm, null);
    }

    /**
     * Evaluate risk and return the {@link Risk} object.
     * Never returns null - return {@link Risk#invalid(String)} instead.
     *
     * @return risk object
     */
    public abstract Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser);

    @Override
    public boolean allowRetries() {
        return true;
    }

    @Override
    public void evaluateRisk(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        this.risk = evaluate(realm, knownUser);
    }

    @Override
    public void close() {

    }
}
