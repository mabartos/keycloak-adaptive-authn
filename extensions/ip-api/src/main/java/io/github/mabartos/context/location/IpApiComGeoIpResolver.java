package io.github.mabartos.context.location;

import io.github.mabartos.context.ip.IPAddress;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * GeoIP via <a href="https://ip-api.com">ip-api.com</a>.
 * <ul>
 *   <li><strong>Free</strong> ({@value #RESOLVER_ID_FREE}, no key): {@code http://ip-api.com/json/{ip}} — non-commercial,
 *       ~45 req/min per client IP. Uses plain HTTP; user IP addresses are transmitted unencrypted and responses can be
 *       tampered with in transit. Not suitable for production; prefer {@value #RESOLVER_ID_PRO} (HTTPS) or another resolver.
 *       Each lookup logs WARN with realm and IP.</li>
 *   <li><strong>Pro</strong> ({@value #RESOLVER_ID_PRO}, API key): {@code https://pro.ip-api.com/json/{ip}?key=...} — SSL, commercial use.</li>
 * </ul>
 */
final class IpApiComGeoIpResolver implements GeoIpResolver {
    static final String RESOLVER_ID_FREE = "ip-api-com-free";
    static final String RESOLVER_ID_PRO = "ip-api-com-pro";

    private static final String FREE_ORIGIN = "http://ip-api.com";
    private static final String PRO_ORIGIN = "https://pro.ip-api.com";

    private static final Logger log = Logger.getLogger(IpApiComGeoIpResolver.class);

    private final String resolverId;
    @Nullable
    private final String apiKey;

    IpApiComGeoIpResolver(@Nonnull String resolverId, @Nullable String apiKey) {
        this.resolverId = resolverId;
        this.apiKey = apiKey != null && !apiKey.isBlank() ? apiKey.trim() : null;
    }

    @Override
    @Nonnull
    public String id() {
        return resolverId;
    }

    @Override
    public Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip) {
        if (apiKey == null) {
            String realmName = realm != null ? realm.getName() : "";
            log.warnf(
                    "GeoIP resolver %s uses plain HTTP for lookup (realm=%s, ip=%s); "
                            + "user IP addresses are transmitted unencrypted — not suitable for production",
                    RESOLVER_ID_FREE,
                    realmName,
                    ip);
        }
        try {
            String origin = apiKey != null ? PRO_ORIGIN : FREE_ORIGIN;
            String path = "/json/" + GeoIpHttp.encodeIpForPath(ip);
            URIBuilder ub = new URIBuilder(origin).setPath(path);
            if (apiKey != null) {
                ub.addParameter("key", apiKey);
            }
            URI uri = ub.build();
            return GeoIpHttp.getJsonStream(session, uri).map(stream -> {
                try {
                    return JsonSerialization.readValue(stream, IpApiComLocationData.class);
                } catch (Exception e) {
                    log.warnf(e, "ip-api.com JSON parse failed for %s", ip);
                    return null;
                }
            }).flatMap(IpApiComGeoIpResolver::toLocationOrEmpty);
        } catch (URISyntaxException e) {
            log.errorf(e, "Invalid ip-api.com URI (origin=%s)", apiKey != null ? PRO_ORIGIN : FREE_ORIGIN);
            return Optional.empty();
        }
    }

    static Optional<LocationData> toLocationOrEmpty(IpApiComLocationData data) {
        if (data == null) {
            return Optional.empty();
        }
        if (!data.isSuccess()) {
            log.tracef("ip-api.com lookup failed: %s", data.getStatusMessage());
            return Optional.empty();
        }
        if (!StringUtil.isNotBlank(data.getCountry())) {
            return Optional.empty();
        }
        return Optional.of(data);
    }
}
