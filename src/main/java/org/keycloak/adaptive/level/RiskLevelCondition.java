package org.keycloak.adaptive.level;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.keycloak.adaptive.spi.engine.RiskEngine.RISK_AUTH_NOTE;

public class RiskLevelCondition implements ConditionalAuthenticator {
    private static final Logger logger = Logger.getLogger(RiskLevelCondition.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        logger.debugf("RISK LEVEL");
        logger.debugf(context.getAuthenticationSession().getAuthNote(RISK_AUTH_NOTE));
        return true;
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
