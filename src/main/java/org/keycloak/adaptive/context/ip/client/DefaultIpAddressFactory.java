package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class DefaultIpAddressFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "default-ip-address-context";

    @Override
    public IpAddressContext create(KeycloakSession session) {
        return new DefaultIpAddress(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
