package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

public final class IpApiComGeoIpResolverFreeFactory implements GeoIpResolverFactory {

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        return new IpApiComGeoIpResolver(GeoIpResolverIds.IP_API_COM_FREE, null);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IP_API_COM_FREE;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true;
    }
}
