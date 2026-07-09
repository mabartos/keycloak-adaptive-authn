package io.github.mabartos.context.location.geoip;

import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Reads {@code KC_ADAPTIVE_*} settings through Keycloak's Quarkus {@link Configuration} API.
 *
 * <p>Property keys use Keycloak's kebab-case form (as shown by {@code kc.sh show-config}), which
 * matches {@code KC_*} environment variables.</p>
 */
public final class AdaptiveConfig {

    /** {@code KC_ADAPTIVE_LOCATION_PROVIDERS} */
    public static final String LOCATION_PROVIDERS_PROPERTY = "kc.adaptive-location-providers";

    /** {@code KC_ADAPTIVE_IPAPI_TOKEN} */
    public static final String IPAPI_TOKEN_PROPERTY = "kc.adaptive-ipapi-token";

    /** {@code KC_ADAPTIVE_IP_API_COM_API_KEY} */
    public static final String IP_API_COM_API_KEY_PROPERTY = "kc.adaptive-ip-api-com-api-key";

    /** {@code KC_ADAPTIVE_MAXMIND_ACCOUNT_ID} */
    public static final String MAXMIND_ACCOUNT_ID_PROPERTY = "kc.adaptive-maxmind-account-id";

    /** {@code KC_ADAPTIVE_MAXMIND_LICENSE_KEY} */
    public static final String MAXMIND_LICENSE_KEY_PROPERTY = "kc.adaptive-maxmind-license-key";

    /** {@code KC_ADAPTIVE_MAXMIND_DB_REFRESH_INTERVAL} */
    public static final String MAXMIND_DB_REFRESH_INTERVAL_PROPERTY = "kc.adaptive-maxmind-db-refresh-interval";

    /** {@code KC_ADAPTIVE_MAXMIND_DB_PATH} */
    public static final String MAXMIND_DB_PATH_PROPERTY = "kc.adaptive-maxmind-db-path";

    private static final Duration DEFAULT_MAXMIND_DB_REFRESH_INTERVAL = Duration.ofDays(7);

    private AdaptiveConfig() {
    }

    public static Optional<String> locationProviders() {
        return getOptionalValue(LOCATION_PROVIDERS_PROPERTY);
    }

    public static Optional<String> ipApiCoToken() {
        return getOptionalValue(IPAPI_TOKEN_PROPERTY);
    }

    public static Optional<String> ipApiComApiKey() {
        return getOptionalValue(IP_API_COM_API_KEY_PROPERTY);
    }

    public static Optional<String> maxMindAccountId() {
        return getOptionalValue(MAXMIND_ACCOUNT_ID_PROPERTY);
    }

    public static Optional<String> maxMindLicenseKey() {
        return getOptionalValue(MAXMIND_LICENSE_KEY_PROPERTY);
    }

    public static Duration maxMindDbRefreshInterval() {
        return getOptionalValue(MAXMIND_DB_REFRESH_INTERVAL_PROPERTY)
                .map(AdaptiveConfig::parseDuration)
                .filter(duration -> !duration.isNegative() && !duration.isZero())
                .orElse(DEFAULT_MAXMIND_DB_REFRESH_INTERVAL);
    }

    public static Path maxMindDbPath() {
        return maxMindDbPathOptional().orElseGet(AdaptiveConfig::defaultMaxMindDbPath);
    }

    public static Optional<Path> maxMindDbPathOptional() {
        return getOptionalValue(MAXMIND_DB_PATH_PROPERTY).map(Path::of);
    }

    public static boolean maxMindOfficialCredentialsPresent() {
        return maxMindAccountId().isPresent() && maxMindLicenseKey().isPresent();
    }

    public static boolean maxMindPartialCredentialsPresent() {
        return maxMindAccountId().isPresent() ^ maxMindLicenseKey().isPresent();
    }

    private static Path defaultMaxMindDbPath() {
        String dataDir = getOptionalValue("kc.data-dir").orElse(System.getProperty("java.io.tmpdir"));
        return Path.of(dataDir, "adaptive-authn", "geolite2", "GeoLite2-City.mmdb");
    }

    private static Duration parseDuration(String raw) {
        return Duration.parse(raw.trim());
    }

    private static Optional<String> getOptionalValue(String key) {
        return Configuration.getOptionalValue(key)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
