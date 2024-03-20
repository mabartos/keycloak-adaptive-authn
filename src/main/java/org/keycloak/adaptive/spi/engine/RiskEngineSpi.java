package org.keycloak.adaptive.spi.engine;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RiskEngineSpi implements Spi {
    public static final String SPI_NAME = "risk-engine-spi";

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
        return RiskEngine.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RiskEngineFactory.class;
    }
}
