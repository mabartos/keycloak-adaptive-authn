package io.github.mabartos.context.location;

import io.github.mabartos.spi.context.UserContextFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * User-context factory for modular GeoIP lookup.
 *
 * <p>Settings are read from Quarkus {@code application.properties} (or env overrides), not from SPI {@code keycloak.conf} keys.</p>
 * <ul>
 *   <li>{@value #PROVIDERS_PROPERTY} — comma-separated resolver ids (try order). Default: {@value IpapiCoGeoIpResolver#RESOLVER_ID_FREE}.</li>
 *   <li>Known ids: {@code ipapi-co-free}, {@code ipapi-co-pro}, {@code ip-api-com-free}, {@code ip-api-com-pro}.</li>
 *   <li>{@value #IPAPI_TOKEN_PROPERTY} — required for {@code ipapi-co-pro}; ignored for {@code ipapi-co-free}.</li>
 *   <li>{@value #IP_API_COM_API_KEY_PROPERTY} — required for {@code ip-api-com-pro}; ignored for {@code ip-api-com-free}.</li>
 *   <li>If every resolver fails, {@link GeoIpLocationContext} returns {@link UnknownLocationData} (string fields {@link UnknownLocationData#UNKNOWN}, not cached).</li>
 * </ul>
 */
public class GeoIpLocationContextFactory implements UserContextFactory<LocationContext> {

    public static final String PROVIDER_ID = "ip-api-location-context";

    public static final String PROVIDERS_PROPERTY = "location.providers";
    public static final String IPAPI_TOKEN_PROPERTY = "location.ipapi.token";
    public static final String IP_API_COM_API_KEY_PROPERTY = "location.ip-api-com.api-key";

    private static final Logger log = Logger.getLogger(GeoIpLocationContextFactory.class);

    private List<GeoIpResolver> resolvers = List.of(new IpapiCoGeoIpResolver(IpapiCoGeoIpResolver.RESOLVER_ID_FREE, null));

    @Override
    public void init(Config.Scope config) {
        this.resolvers = List.copyOf(buildResolvers());
    }

    @Override
    public GeoIpLocationContext create(KeycloakSession session) {
        return new GeoIpLocationContext(session, resolvers);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<LocationContext> getUserContextClass() {
        return LocationContext.class;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    private static String readIpapiToken() {
        return Configuration.getOptionalValue(IPAPI_TOKEN_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private static String readIpApiComKey() {
        return Configuration.getOptionalValue(IP_API_COM_API_KEY_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private static List<GeoIpResolver> buildResolvers() {
        String raw = Configuration.getOptionalValue(PROVIDERS_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(IpapiCoGeoIpResolver.RESOLVER_ID_FREE);
        List<GeoIpResolver> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            switch (id) {
                case IpapiCoGeoIpResolver.RESOLVER_ID_FREE:
                    list.add(new IpapiCoGeoIpResolver(IpapiCoGeoIpResolver.RESOLVER_ID_FREE, null));
                    break;
                case IpapiCoGeoIpResolver.RESOLVER_ID_PRO: {
                    String token = readIpapiToken();
                    if (token == null) {
                        log.warnf(
                                "GeoIP providers include '%s' but '%s' is blank; skipping that resolver.",
                                IpapiCoGeoIpResolver.RESOLVER_ID_PRO,
                                IPAPI_TOKEN_PROPERTY);
                        break;
                    }
                    list.add(new IpapiCoGeoIpResolver(IpapiCoGeoIpResolver.RESOLVER_ID_PRO, token));
                    break;
                }
                case IpApiComGeoIpResolver.RESOLVER_ID_FREE:
                    list.add(new IpApiComGeoIpResolver(IpApiComGeoIpResolver.RESOLVER_ID_FREE, null));
                    break;
                case IpApiComGeoIpResolver.RESOLVER_ID_PRO: {
                    String key = readIpApiComKey();
                    if (key == null) {
                        log.warnf(
                                "GeoIP providers include '%s' but '%s' is blank; skipping that resolver.",
                                IpApiComGeoIpResolver.RESOLVER_ID_PRO,
                                IP_API_COM_API_KEY_PROPERTY);
                        break;
                    }
                    list.add(new IpApiComGeoIpResolver(IpApiComGeoIpResolver.RESOLVER_ID_PRO, key));
                    break;
                }
                default:
                    log.warnf("Unknown GeoIP resolver id '%s' in %s=%s", id, PROVIDERS_PROPERTY, raw);
            }
        }
        if (list.isEmpty()) {
            log.warnf("No usable GeoIP resolvers after parsing %s=%s; falling back to %s.",
                    PROVIDERS_PROPERTY, raw, IpapiCoGeoIpResolver.RESOLVER_ID_FREE);
            list.add(new IpapiCoGeoIpResolver(IpapiCoGeoIpResolver.RESOLVER_ID_FREE, null));
        }
        return list;
    }
}
