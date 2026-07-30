package io.github.mabartos.context.location.geoip;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves the ordered GeoIP resolver chain from {@link AdaptiveConfig#LOCATION_PROVIDERS_PROPERTY}
 * while honouring Keycloak's provider registry ({@link KeycloakSession#getProvider(Class, String)}).
 *
 * <p>Provider order is user-controlled via config; SPI factory {@code order()} is not used for
 * fallback sequencing. Pro-tier resolvers are registered at Keycloak build time and filtered here
 * at runtime when their credential env vars are absent.</p>
 */
public final class GeoIpResolverChain {

    /** @deprecated use {@link AdaptiveConfig#LOCATION_PROVIDERS_PROPERTY} */
    @Deprecated
    public static final String PROVIDERS_PROPERTY = AdaptiveConfig.LOCATION_PROVIDERS_PROPERTY;

    /** @deprecated use {@link AdaptiveConfig#IPAPI_TOKEN_PROPERTY} */
    @Deprecated
    public static final String IPAPI_TOKEN_PROPERTY = AdaptiveConfig.IPAPI_TOKEN_PROPERTY;

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
                        AdaptiveConfig.LOCATION_PROVIDERS_PROPERTY,
                        raw);
                continue;
            }
            GeoIpResolver resolver = providerById.apply(providerId);
            if (resolver == null) {
                log.warnf(
                        "GeoIP resolver id '%s' from %s=%s is not registered; skipping.",
                        providerId,
                        AdaptiveConfig.LOCATION_PROVIDERS_PROPERTY,
                        raw);
                continue;
            }
            if (!hasCredentialsFor(providerId)) {
                log.warnf(
                        "GeoIP resolver id '%s' from %s=%s is not available (missing credentials); skipping.",
                        providerId,
                        AdaptiveConfig.LOCATION_PROVIDERS_PROPERTY,
                        raw);
                continue;
            }
            chain.add(resolver);
        }

        if (chain.isEmpty()) {
            log.warnf(
                    "No usable GeoIP resolvers after parsing %s=%s; falling back to %s.",
                    AdaptiveConfig.LOCATION_PROVIDERS_PROPERTY,
                    raw,
                    GeoIpResolverIds.DEFAULT_FALLBACK);
            GeoIpResolver fallback = providerById.apply(GeoIpResolverIds.DEFAULT_FALLBACK);
            if (fallback != null && hasCredentialsFor(GeoIpResolverIds.DEFAULT_FALLBACK)) {
                chain.add(fallback);
            }
        }
        return List.copyOf(chain);
    }

    static boolean hasCredentialsFor(String providerId) {
        return switch (providerId) {
            case GeoIpResolverIds.IPAPI_CO_PRO -> readIpApiToken() != null;
            case GeoIpResolverIds.IP_API_COM_PRO -> readIpApiComApiKey() != null;
            default -> true;
        };
    }

    /**
     * Preserves legacy behaviour: deployments that set only {@link AdaptiveConfig#IPAPI_TOKEN_PROPERTY} (no
     * {@link AdaptiveConfig#LOCATION_PROVIDERS_PROPERTY}) default to {@value GeoIpResolverIds#IPAPI_CO_PRO}.
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
     * True when {@link AdaptiveConfig#LOCATION_PROVIDERS_PROPERTY} is set in SmallRye Config (env, properties file, etc.).
     */
    static boolean isProvidersExplicitlyConfigured(String configuredProviders) {
        return configuredProviders != null && !configuredProviders.isBlank();
    }

    /**
     * True when {@code providerId} appears in the configured provider list (CSV parsing + legacy migration rules).
     */
    public static boolean isProviderConfigured(String providerId) {
        String configured = readConfiguredProviders();
        String ipApiToken = readIpApiToken();
        String raw = resolveProvidersForMigration(configured, isProvidersExplicitlyConfigured(configured), ipApiToken);
        return parseOrderedProviderIds(raw).contains(providerId);
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
        return AdaptiveConfig.locationProviders().orElse(null);
    }

    private static String readIpApiToken() {
        return AdaptiveConfig.ipApiCoToken().orElse(null);
    }

    private static String readIpApiComApiKey() {
        return AdaptiveConfig.ipApiComApiKey().orElse(null);
    }
}
