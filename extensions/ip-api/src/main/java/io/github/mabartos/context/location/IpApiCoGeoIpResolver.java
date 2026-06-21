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
 * GeoIP via <a href="https://ipapi.co">ipapi.co</a> ({@code GET https://ipapi.co/{ip}/json/?token=...}).
 * <p>Resolver id (see {@link #id()}) distinguishes {@value #RESOLVER_ID_FREE} (no token) from {@value #RESOLVER_ID_PRO} (token required when building the chain).</p>
 */
final class IpApiCoGeoIpResolver implements GeoIpResolver {
    static final String RESOLVER_ID_FREE = "ipapi-co-free";
    static final String RESOLVER_ID_PRO = "ipapi-co-pro";

    private static final String ORIGIN = "https://ipapi.co";

    private static final Logger log = Logger.getLogger(IpApiCoGeoIpResolver.class);

    private final String resolverId;
    @Nullable
    private final String token;

    IpApiCoGeoIpResolver(@Nonnull String resolverId, @Nullable String token) {
        this.resolverId = resolverId;
        this.token = token != null && !token.isBlank() ? token.trim() : null;
    }

    @Override
    @Nonnull
    public String id() {
        return resolverId;
    }

    @Override
    public Optional<LocationData> resolve(KeycloakSession session, RealmModel realm, IPAddress ip) {
        try {
            String path = "/" + GeoIpHttp.encodeIpForPath(ip) + "/json/";
            URIBuilder ub = new URIBuilder(ORIGIN).setPath(path);
            if (token != null) {
                ub.addParameter("token", token);
            }
            URI uri = ub.build();
            return GeoIpHttp.getJsonStream(session, uri).flatMap(stream -> {
                try {
                    return toLocationOrEmpty(JsonSerialization.readValue(stream, IpApiLocationData.class));
                } catch (Exception e) {
                    log.warnf(e, "ipapi.co JSON parse failed for %s", ip);
                    return Optional.empty();
                }
            });
        } catch (URISyntaxException e) {
            log.errorf(e, "Invalid ipapi.co URI");
            return Optional.empty();
        }
    }

    static Optional<LocationData> toLocationOrEmpty(IpApiLocationData data) {
        if (data == null || !StringUtil.isNotBlank(data.getCountry())) {
            return Optional.empty();
        }
        return Optional.of(data);
    }
}
