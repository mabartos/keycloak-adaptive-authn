package io.github.mabartos.context.ip.client;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.IpAddressUtils;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class TestIpAddressContext extends IpAddressContext {
    private static final Logger log = Logger.getLogger(TestIpAddressContext.class);

    public static final String TESTING_IP = "77.75.72.3"; // seznam.cz
    public static final String USE_TESTING_IP_PROP = "ip.address.use.testing";
    public static final String TESTING_IP_VALUE_PROP = "ip.address.testing.value";
    public static final String TESTING_IP_RANDOM_PROP = "ip.address.testing.random.enabled";

    private static final List<String> TESTING_IPS = List.of(
            "176.150.253.172", // France
            "191.101.157.70",  // Germany
            "79.142.79.54",    // Switzerland
            "212.112.19.28",   // Sweden
            "212.102.49.216"   // Spain
    );

    public TestIpAddressContext(KeycloakSession session) {
        super(session);
    }

    public static boolean isTestIpAddressUsed() {
        return resolveTestingIp().isPresent();
    }

    @Override
    public Optional<IPAddress> initData(@Nonnull RealmModel realm) {
        return resolveTestingIp().flatMap(IpAddressUtils::getIpAddress);
    }

    private static Optional<String> resolveTestingIp() {
        List<String> configuredIps = getConfiguredTestingIps();

        if (!configuredIps.isEmpty()) {
            boolean useRandomIp = isRandomTestingIpEnabled();
            if (useRandomIp && configuredIps.size() > 1) {
                String ip = configuredIps.get(ThreadLocalRandom.current().nextInt(configuredIps.size()));
                log.tracef("Using random configured testing IP: %s", ip);
                return Optional.of(ip);
            }

            String ip = configuredIps.getFirst();
            log.tracef("Using configured testing IP: %s", ip);
            return Optional.of(ip);
        }

        boolean useRandomIp = isRandomTestingIpEnabled();
        if (useRandomIp && !TESTING_IPS.isEmpty()) {
            String ip = TESTING_IPS.get(ThreadLocalRandom.current().nextInt(TESTING_IPS.size()));
            log.tracef("Using random testing IP: %s", ip);
            return Optional.of(ip);
        }

        if (isLegacyTestingIpEnabled()) {
            return Optional.of(TESTING_IP);
        }

        return Optional.empty();
    }

    private static boolean isLegacyTestingIpEnabled() {
        return getConfigValue(USE_TESTING_IP_PROP)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private static boolean isRandomTestingIpEnabled() {
        return getConfigValue(TESTING_IP_RANDOM_PROP)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private static List<String> getConfiguredTestingIps() {
        return getConfigValue(TESTING_IP_VALUE_PROP)
                .stream()
                .flatMap(value -> Stream.of(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(TestIpAddressContext::isValidIp)
                .toList();
    }

    private static Optional<String> getConfigValue(String key) {
        return Configuration.getOptionalValue(key)
                .or(() -> Optional.ofNullable(System.getProperty(key)))
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private static boolean isValidIp(String ip) {
        boolean valid = IPAddress.parse(ip) != null;
        if (!valid) {
            log.warnf("Invalid configured testing IP: %s", ip);
        }
        return valid;
    }
}
