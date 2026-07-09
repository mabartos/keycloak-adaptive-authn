package io.github.mabartos.context.location.geoip;

import org.keycloak.quarkus.runtime.configuration.Configuration;

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

    private static Optional<String> getOptionalValue(String key) {
        return Configuration.getOptionalValue(key)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
