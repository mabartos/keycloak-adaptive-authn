package io.github.mabartos.context.location.maxmind;

import io.github.mabartos.context.location.geoip.GeoIpResolver;
import io.github.mabartos.context.location.geoip.GeoIpResolverFactory;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for the local MaxMind GeoLite2-City resolver ({@value GeoIpResolverIds#MAXMIND}).
 */
public final class MaxMindGeoIpResolverFactory implements GeoIpResolverFactory {

    @Override
    public GeoIpResolver create(KeycloakSession session) {
        return new MaxMindGeoIpResolver();
    }

    @Override
    public String getId() {
        return GeoIpResolverIds.MAXMIND;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        MaxMindDatabaseManager.getInstance().startIfEnabled();
    }

    @Override
    public void close() {
        MaxMindDatabaseManager.getInstance().shutdown();
    }
}
