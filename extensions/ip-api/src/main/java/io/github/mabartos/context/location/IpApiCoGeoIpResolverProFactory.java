package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.AdaptiveConfig;
import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.models.KeycloakSession;

public final class IpApiCoGeoIpResolverProFactory implements GeoIpResolverFactory {

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        String token = AdaptiveConfig.ipApiCoToken().orElse(null);
        return new IpApiCoGeoIpResolver(GeoIpResolverIds.IPAPI_CO_PRO, token);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IPAPI_CO_PRO;
    }
}
