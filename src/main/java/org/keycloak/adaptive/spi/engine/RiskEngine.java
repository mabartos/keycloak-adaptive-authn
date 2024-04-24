package org.keycloak.adaptive.spi.engine;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

public interface RiskEngine extends Authenticator {
    String RISK_NO_USER_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_RISK_NO_USER";
    String RISK_REQUIRES_USER_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_RISK_REQUIRES_USER";
    String RISK_OVERALL_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_OVERALL_RISK";

    Double getRisk();

    Set<UserContext<?>> getRiskFactors();

    Set<RiskEvaluator> getRiskEvaluators();

    void evaluateRisk();

    @Override
    default void action(AuthenticationFlowContext context) {
    }

    @Override
    default boolean requiresUser() {
        return false;
    }

    @Override
    default boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    default void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    default void close() {

    }

    static boolean isValidValue(Double value) {
        if (value == null) return false;
        return value >= 0.0d && value <= 1.0d;
    }
}
