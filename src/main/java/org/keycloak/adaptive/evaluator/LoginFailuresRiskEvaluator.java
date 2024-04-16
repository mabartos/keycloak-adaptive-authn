package org.keycloak.adaptive.evaluator;

import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

public class LoginFailuresRiskEvaluator implements RiskEvaluator {
    private final KeycloakSession session;
    private Double risk;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public void evaluate() {
        var realm = session.getContext().getRealm();
        if (realm == null) return;

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) return;

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) return;

        if (loginFailures.getNumFailures() == 0) {
            this.risk = Risk.NONE;
        }

        // TODO compute num of failures
        // Get maximum of possible num failures - realm brute force setting
        // relatively compute it

        // TODO compute when was the last login failure
        // TODO analyze IP address
    }
}
