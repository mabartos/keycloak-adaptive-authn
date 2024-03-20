package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RiskFactorEvaluatorSpi implements Spi {

    public static final String SPI_NAME = "risk-factor-evaluator";

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
        return RiskFactorEvaluator.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RiskFactorEvaluatorFactory.class;
    }
}
