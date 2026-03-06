package io.github.mabartos.context.ip.client;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class TestIpAddressContextFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "test-ip-address-context";

    @Override
    public TestIpAddressContext create(KeycloakSession session) {
        return new TestIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<IpAddressContext> getUserContextClass() {
        return IpAddressContext.class;
    }

    @Override
    public int getPriority() {
        return 999;
    }
}
