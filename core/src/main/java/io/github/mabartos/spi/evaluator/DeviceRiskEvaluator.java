package io.github.mabartos.spi.evaluator;

import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.BEFORE_AUTHN;

@EvaluationPhase(BEFORE_AUTHN)
public abstract class DeviceRiskEvaluator extends AbstractRiskEvaluator {

    public abstract Risk evaluate(@Nonnull RealmModel realm);

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        return evaluate(realm);
    }
}
