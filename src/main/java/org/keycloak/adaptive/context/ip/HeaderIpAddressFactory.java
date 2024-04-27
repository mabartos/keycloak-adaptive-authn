package org.keycloak.adaptive.context.ip;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class HeaderIpAddressFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "header-ip-address-context";

    @Override
    public IpAddressContext create(KeycloakSession session) {
        return new HeaderIpAddressProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
