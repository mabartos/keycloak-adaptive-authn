package org.keycloak.adaptive.context.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.factor.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class HeaderUserAgentContextFactory implements UserContextFactory<UserAgentContext> {
    public static final String PROVIDER_ID = "default-user-agent-risk-factor";

    @Override
    public UserAgentContext create(KeycloakSession session) {
        return new HeaderUserAgentContext(session);
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
