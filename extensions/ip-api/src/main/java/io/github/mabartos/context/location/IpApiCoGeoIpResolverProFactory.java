package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverChain;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public final class IpApiCoGeoIpResolverProFactory implements GeoIpResolverFactory {

    private String token;

    @Override
    public void init(Config.Scope config) {
        this.token = readToken();
    }

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        return new IpApiCoGeoIpResolver(GeoIpResolverIds.IPAPI_CO_PRO, token);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IPAPI_CO_PRO;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return readToken() != null;
    }

    private static String readToken() {
        return Configuration.getOptionalValue(GeoIpResolverChain.IPAPI_TOKEN_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}
