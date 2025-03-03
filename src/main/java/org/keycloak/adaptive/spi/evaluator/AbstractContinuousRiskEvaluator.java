package org.keycloak.adaptive.spi.evaluator;

import org.keycloak.adaptive.level.Risk;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

public abstract class AbstractContinuousRiskEvaluator extends AbstractRiskEvaluator implements ContinuousRiskEvaluator {

    @Override
    public void evaluateRisk(RealmModel realm, UserModel user) {
        this.risk = evaluate(realm, user);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.CONTINUOUS);
    }

    public abstract Risk evaluate(RealmModel realm, UserModel user);

    @Override
    public Risk evaluate() {
        return evaluate(null, null);
    }
}
