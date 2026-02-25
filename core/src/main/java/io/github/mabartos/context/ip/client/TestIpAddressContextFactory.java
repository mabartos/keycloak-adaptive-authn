package io.github.mabartos.context.ip.client;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class TestIpAddressContextFactory implements UserContextFactory<TestIpAddressContext> {
    public static final String PROVIDER_ID = "test-ip-address-context";
    public static final String USE_TESTING_IP_PROP = "ip.address.use.testing";

    @Override
    public TestIpAddressContext create(KeycloakSession session) {
        return new TestIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
