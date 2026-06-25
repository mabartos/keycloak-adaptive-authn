package io.github.mabartos.context.location.geoip;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for a single GeoIP backend ({@link GeoIpResolver#id()} matches {@link #getId()}).
 */
public interface GeoIpResolverFactory extends ProviderFactory<GeoIpResolver>, EnvironmentDependentProviderFactory {

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }
}
