package org.keycloak.adaptive.spi.condition;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface UserContextCondition extends Provider, ConditionalAuthenticator {
    @Override
    default void close() {
    }

    @Override
    default void action(AuthenticationFlowContext context) {

    }

    @Override
    default boolean requiresUser() {
        return true;
    }

    @Override
    default void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

}
