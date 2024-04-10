package org.keycloak.adaptive.context;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.factor.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DeviceContextFactory implements UserContextFactory<DeviceContext> {
    public static final String PROVIDER_ID = "default-user-agent-risk-factor";

    @Override
    public DeviceContext create(KeycloakSession session) {
        return new DeviceContextProvider(session);
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
