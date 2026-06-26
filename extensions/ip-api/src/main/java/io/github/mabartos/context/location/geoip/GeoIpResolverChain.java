package io.github.mabartos.context.location.geoip;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves the ordered GeoIP resolver chain from {@value #PROVIDERS_PROPERTY} while honouring
 * Keycloak's provider registry ({@link KeycloakSession#getProvider(Class, String)}).
 *
 * <p>Provider order is user-controlled via config; SPI factory {@code order()} is not used for
 * fallback sequencing. Enabled factories are looked up by id in the configured order.</p>
 */
public final class GeoIpResolverChain {

    public static final String PROVIDERS_PROPERTY = "kc.adaptive.location.providers";
    public static final String IPAPI_TOKEN_PROPERTY = "kc.adaptive.ipapi.token";

    private static final Logger log = Logger.getLogger(GeoIpResolverChain.class);

    private static volatile List<String> orderedProviderIds = List.of(GeoIpResolverIds.DEFAULT_FALLBACK);

    private GeoIpResolverChain() {
    }

    /**
     * Reads config and caches the ordered provider id list. Call from a factory {@code init()} hook.
     */
    public static void configure() {
        String configured = readConfiguredProviders();
        String ipApiToken = readIpApiToken();
        String raw = resolveProvidersForMigration(configured, isProvidersExplicitlyConfigured(configured), ipApiToken);
        orderedProviderIds = List.copyOf(parseOrderedProviderIds(raw));
    }

    /**
     * Returns enabled {@link GeoIpResolver} providers in configured try order.
     */
    public static List<GeoIpResolver> resolve(KeycloakSession session) {
        return buildChain(orderedProviderIds, id -> session.getProvider(GeoIpResolver.class, id));
    }

    static List<GeoIpResolver> buildChain(
            List<String> providerIds, java.util.function.Function<String, GeoIpResolver> providerById) {
        List<GeoIpResolver> chain = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        String raw = String.join(",", providerIds);

        for (String providerId : providerIds) {
            if (!seenIds.add(providerId)) {
                log.warnf(
                        "Duplicate GeoIP resolver id '%s' in %s=%s; skipping.",
                        providerId,
                        PROVIDERS_PROPERTY,
                        raw);
                continue;
            }
            GeoIpResolver resolver = providerById.apply(providerId);
            if (resolver == null) {
                log.warnf(
                        "GeoIP resolver id '%s' from %s=%s is not available (disabled or missing credentials); skipping.",
                        providerId,
                        PROVIDERS_PROPERTY,
                        raw);
                continue;
            }
            chain.add(resolver);
        }

        if (chain.isEmpty()) {
            log.warnf(
                    "No usable GeoIP resolvers after parsing %s=%s; falling back to %s.",
                    PROVIDERS_PROPERTY,
                    raw,
                    GeoIpResolverIds.DEFAULT_FALLBACK);
            GeoIpResolver fallback = providerById.apply(GeoIpResolverIds.DEFAULT_FALLBACK);
            if (fallback != null) {
                chain.add(fallback);
            }
        }
        return List.copyOf(chain);
    }

    /**
     * Preserves legacy behaviour: deployments that set only {@value #IPAPI_TOKEN_PROPERTY} (no
     * {@value #PROVIDERS_PROPERTY}) default to {@value GeoIpResolverIds#IPAPI_CO_PRO}.
     */
    static String resolveProvidersForMigration(
            String configuredProviders, boolean providersExplicitlySet, String ipApiToken) {
        String raw = configuredProviders != null && !configuredProviders.isBlank()
                ? configuredProviders.trim()
                : GeoIpResolverIds.DEFAULT_FALLBACK;
        if (!providersExplicitlySet && ipApiToken != null && !ipApiToken.isBlank()) {
            return GeoIpResolverIds.IPAPI_CO_PRO;
        }
        return raw;
    }

    /**
     * True when {@value #PROVIDERS_PROPERTY} is set in SmallRye Config (env, properties file, etc.).
     */
    static boolean isProvidersExplicitlyConfigured(String configuredProviders) {
        return configuredProviders != null && !configuredProviders.isBlank();
    }

    /**
     * Parses comma-separated provider ids (lower-cased, blanks skipped).
     */
    static List<String> parseOrderedProviderIds(String providersRaw) {
        String raw = providersRaw != null && !providersRaw.isBlank()
                ? providersRaw.trim()
                : GeoIpResolverIds.DEFAULT_FALLBACK;
        List<String> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            ids.add(GeoIpResolverIds.DEFAULT_FALLBACK);
        }
        return ids;
    }

    private static String readConfiguredProviders() {
        return Configuration.getOptionalValue(PROVIDERS_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private static String readIpApiToken() {
        return Configuration.getOptionalValue(IPAPI_TOKEN_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}
