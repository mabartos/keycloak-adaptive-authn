package org.keycloak.adaptive.context.role;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserRoleContextFactory implements UserContextFactory<UserRoleContext> {
    public static final String PROVIDER_ID = "kc-user-role-risk-factor";

    @Override
    public UserRoleContext create(KeycloakSession session) {
        return new UserRoleContextProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
