package org.keycloak.adaptive.evaluator;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

public class LoginFailuresRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(LoginFailuresRiskEvaluator.class);

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
        if (realm == null) {
            logger.debug("Context realm is null");
            return;
        }

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) {
            logger.debug("Context user is null");
            return;
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) {
            logger.debug("Cannot obtain login failures");
            return;
        }

        // TODO compute num of failures
        // Get maximum of possible num failures - realm brute force setting
        // relatively compute it
        var numFailures = loginFailures.getNumFailures();
        if (numFailures <= 2) {
            this.risk = Risk.NONE;
        } else if (numFailures <= 5) {
            this.risk = Risk.SMALL;
        } else if (numFailures < 10) {
            this.risk = Risk.MEDIUM;
        } else if (numFailures < 15) {
            this.risk = Risk.INTERMEDIATE;
        } else {
            this.risk = Risk.HIGH;
        }

        // TODO compute when was the last login failure
        // TODO analyze IP address
    }
}
