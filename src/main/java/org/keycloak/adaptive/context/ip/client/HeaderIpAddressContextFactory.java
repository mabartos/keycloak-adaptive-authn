package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class HeaderIpAddressContextFactory implements UserContextFactory<HeaderIpAddressContext> {
    public static final String PROVIDER_ID = "header-ip-address-context";

    @Override
    public HeaderIpAddressContext create(KeycloakSession session) {
        return new HeaderIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
