package io.github.mabartos.context.location;

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public final class IpApiComGeoIpResolverProFactory implements GeoIpResolverFactory {

    public static final String API_KEY_PROPERTY = "kc.adaptive.ip-api-com.api-key";

    private String apiKey;

    @Override
    public void init(Config.Scope config) {
        this.apiKey = readApiKey();
    }

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        return new IpApiComGeoIpResolver(GeoIpResolverIds.IP_API_COM_PRO, apiKey);
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.IP_API_COM_PRO;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return readApiKey() != null;
    }

    private static String readApiKey() {
        return Configuration.getOptionalValue(API_KEY_PROPERTY)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}
