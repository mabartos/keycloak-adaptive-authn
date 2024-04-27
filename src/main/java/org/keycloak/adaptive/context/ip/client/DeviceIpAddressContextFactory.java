package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class DeviceIpAddressContextFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "device-ip-address-context";

    @Override
    public IpAddressContext create(KeycloakSession session) {
        return new DeviceIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
