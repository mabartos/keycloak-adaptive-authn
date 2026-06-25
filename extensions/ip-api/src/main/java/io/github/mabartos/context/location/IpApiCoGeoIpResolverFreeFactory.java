package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

public final class IpApiCoGeoIpResolverFreeFactory implements GeoIpResolverFactory {

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        return new IpApiCoGeoIpResolver(GeoIpResolverIds.IPAPI_CO_FREE, null);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IPAPI_CO_FREE;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true;
    }
}
