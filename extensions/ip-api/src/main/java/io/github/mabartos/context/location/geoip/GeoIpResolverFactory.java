package io.github.mabartos.context.location.geoip;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for a single GeoIP backend ({@link GeoIpResolver#id()} matches {@link #getId()}).
 *
 * <p>Pro-tier factories are always registered at Keycloak build time ({@link #isSupported(Config.Scope)}
 * returns {@code true}). Runtime credential checks happen in {@link GeoIpResolverChain} so secrets
 * supplied only at container start are honoured without a rebuild.</p>
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

    /**
     * Always {@code true}: credential-based enablement is evaluated at runtime by {@link GeoIpResolverChain}.
     */
    @Override
    default boolean isSupported(Config.Scope config) {
        return true;
    }
}
