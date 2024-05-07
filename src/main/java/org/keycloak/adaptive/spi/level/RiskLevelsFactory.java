package org.keycloak.adaptive.spi.level;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.Collections;
import java.util.List;

public interface RiskLevelsFactory extends ProviderFactory<RiskLevelsProvider>, ConfiguredProvider, EnvironmentDependentProviderFactory {

    String getName();

    RiskLevelsProvider getSingleton();

    @Override
    default RiskLevelsProvider create(KeycloakSession session) {
        return getSingleton();
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

    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return true;
    }
}
