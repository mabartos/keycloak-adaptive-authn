package org.keycloak.adaptive.spi.engine;

import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;

public interface RiskEngine extends Authenticator {
    String RISK_AUTH_NOTE = "ADAPTIVE_AUTHN_CURRENT_RISK";

    Double getRiskValue();

    Set<UserContext<?>> getRiskFactors();

    Set<RiskFactorEvaluator> getRiskEvaluators();

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

    static Optional<Double> getStoredRisk(AuthenticationFlowContext context) {
        try {
            return Optional.of(Double.parseDouble(context.getAuthenticationSession().getAuthNote(RISK_AUTH_NOTE)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static void storeRisk(AuthenticationFlowContext context, Double risk) {
        context.getAuthenticationSession().setAuthNote(RISK_AUTH_NOTE, risk.toString());
    }

    default void storeRisk(AuthenticationFlowContext context) {
        storeRisk(context, getRiskValue());
    }
}
