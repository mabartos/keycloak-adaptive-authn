package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class DeviceContextFactory implements UserContextFactory<DeviceContext> {
    public static final String PROVIDER_ID = "kc-device-context";

    @Override
    public DeviceContext create(KeycloakSession session) {
        return new DeviceContextProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
