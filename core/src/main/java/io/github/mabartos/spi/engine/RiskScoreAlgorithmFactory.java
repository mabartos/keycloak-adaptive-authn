package io.github.mabartos.spi.engine;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface RiskScoreAlgorithmFactory extends ProviderFactory<RiskScoreAlgorithm> {

    /**
     * Get name of the algorithm
     */
    String getName();

    /**
     * Get description of the algorithm representing details about risk score calculation
     */
    String getDescription();

    @Override
    default void init(Config.Scope scope) {
        //noop
    }

    @Override
    default void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        //noop
    }

    @Override
    default void close() {
        //noop
    }
}
