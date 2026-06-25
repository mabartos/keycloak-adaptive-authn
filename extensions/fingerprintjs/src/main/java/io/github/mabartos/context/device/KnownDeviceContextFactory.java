package io.github.mabartos.context.device;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class KnownDeviceContextFactory implements UserContextFactory<KnownDeviceContext> {
    public static final String PROVIDER_ID = "known-device-context";

    @Override
    public KnownDeviceContext create(KeycloakSession session) {
        return new KnownDeviceContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<KnownDeviceContext> getUserContextClass() {
        return KnownDeviceContext.class;
    }
}
