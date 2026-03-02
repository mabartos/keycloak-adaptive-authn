package io.github.mabartos.context.ip.client;

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.DeviceContext;
import io.github.mabartos.context.ip.IpAddressUtils;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class TestIpAddressContext extends DeviceContext<IPAddress> {
    private static final String TESTING_IP = "77.75.72.3"; // seznam.cz

    public TestIpAddressContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public Optional<IPAddress> initData(@Nonnull RealmModel realm) {
        return IpAddressUtils.getIpAddress(TESTING_IP);
    }
}
