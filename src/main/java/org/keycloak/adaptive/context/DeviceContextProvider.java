package org.keycloak.adaptive.context;

import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

public class DeviceContextProvider implements DeviceContext {
    private final KeycloakSession session;
    private boolean isInitialized;
    private DeviceRepresentation data;

    public DeviceContextProvider(KeycloakSession session) {
        this.session = session;
        initData();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        this.data = session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation();
        this.isInitialized = true;
    }

    @Override
    public DeviceRepresentation getData() {
        return data;
    }
}
