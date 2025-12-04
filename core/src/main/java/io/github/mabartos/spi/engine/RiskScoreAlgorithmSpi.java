package io.github.mabartos.spi.engine;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RiskScoreAlgorithmSpi implements Spi {

    public static final String SPI_NAME = "risk-score-algorithm";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RiskScoreAlgorithm.class;
    }

    @Override
    public Class<? extends ProviderFactory<?>> getProviderFactoryClass() {
        return RiskScoreAlgorithmFactory.class;
    }
}
