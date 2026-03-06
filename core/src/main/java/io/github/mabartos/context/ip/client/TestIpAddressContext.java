package io.github.mabartos.context.ip.client;

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.DeviceContext;
import io.github.mabartos.context.ip.IpAddressUtils;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class TestIpAddressContext extends IpAddressContext {
    public static final String TESTING_IP = "77.75.72.3"; // seznam.cz
    public static final String USE_TESTING_IP_PROP = "ip.address.use.testing";

    public TestIpAddressContext(KeycloakSession session) {
        super(session);
    }

    public static boolean isTestIpAddressUsed() {
        return Boolean.parseBoolean(System.getProperty(USE_TESTING_IP_PROP, "false"));
    }

    @Override
    public boolean alwaysFetch() {
        return true;
    }

    @Override
    public Optional<IPAddress> initData(@Nonnull RealmModel realm) {
        if (isTestIpAddressUsed()) {
            return IpAddressUtils.getIpAddress(TESTING_IP);
        }
        return Optional.empty();
    }
}
