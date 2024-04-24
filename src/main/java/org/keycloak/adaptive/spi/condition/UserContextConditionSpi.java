package org.keycloak.adaptive.spi.condition;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class UserContextConditionSpi implements Spi {
    public static final String SPI_NAME = "user-context-condition-spi";

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
        return UserContextCondition.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return UserContextConditionFactory.class;
    }
}
