package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class TestIpAddressContextFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "test-ip-address-context";
    public static final String USE_TESTING_IP_PROP = "ip.address.use.testing";

    @Override
    public IpAddressContext create(KeycloakSession session) {
        return new TestIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
