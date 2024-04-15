package org.keycloak.adaptive.spi.factor;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface RiskFactorEvaluatorFactory extends ProviderFactory<RiskFactorEvaluator>, EnvironmentDependentProviderFactory {

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    default void close() {

    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return true;
    }
}
