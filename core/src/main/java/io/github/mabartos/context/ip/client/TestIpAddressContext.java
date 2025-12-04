package io.github.mabartos.context.ip.client;

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.ip.IpAddressUtils;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class TestIpAddressContext extends IpAddressContext {
    private final KeycloakSession session;
    private static final String TESTING_IP = "77.75.72.3"; // seznam.cz

    public TestIpAddressContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<IPAddress> initData() {
        return IpAddressUtils.getIpAddress(TESTING_IP);
    }
}
