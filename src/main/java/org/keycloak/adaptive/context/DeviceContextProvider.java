package org.keycloak.adaptive.context;

import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.models.KeycloakSession;

public class DeviceContextProvider extends DeviceContext {
    private final KeycloakSession session;
    public DeviceContextProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void initData() {
        this.data = session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation();
        this.isInitialized = true;
    }
}
