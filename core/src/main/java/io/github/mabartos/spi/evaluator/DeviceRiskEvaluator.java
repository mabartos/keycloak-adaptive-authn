package io.github.mabartos.spi.evaluator;

import io.github.mabartos.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

public abstract class DeviceRiskEvaluator extends AbstractRiskEvaluator {

    public abstract Risk evaluate(@Nonnull RealmModel realm);

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.BEFORE_AUTHN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        return evaluate(realm);
    }
}
