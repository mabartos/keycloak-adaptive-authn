package io.github.mabartos.spi.evaluator;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Continuous risk evaluator
 */
public interface ContinuousRiskEvaluator extends RiskEvaluator {

    /**
     * Execute evaluation of the risk score for continuous risk evaluator
     *
     * @param realm current realm
     * @param user  known user
     */
    void evaluateRisk(RealmModel realm, UserModel user);
}
