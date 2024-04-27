package org.keycloak.adaptive.spi.context;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface UserContextFactory<T extends Provider> extends ProviderFactory<T> {

    /**
     * Priority for order evaluation of multiple same contexts
     * <p>
     * More higher number, user context is evaluated sooner
     *
     * @return priority
     */
    default int priority() {
        return 0;
    }

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
