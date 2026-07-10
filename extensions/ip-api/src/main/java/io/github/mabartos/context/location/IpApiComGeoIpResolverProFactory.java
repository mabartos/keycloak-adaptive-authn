package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.AdaptiveConfig;
import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.models.KeycloakSession;

public final class IpApiComGeoIpResolverProFactory implements GeoIpResolverFactory {

    /** Keycloak env: {@code KC_ADAPTIVE_IP_API_COM_API_KEY} */
    public static final String API_KEY_PROPERTY = AdaptiveConfig.IP_API_COM_API_KEY_PROPERTY;

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        String apiKey = AdaptiveConfig.ipApiComApiKey().orElse(null);
        return new IpApiComGeoIpResolver(GeoIpResolverIds.IP_API_COM_PRO, apiKey);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IP_API_COM_PRO;
    }
}
