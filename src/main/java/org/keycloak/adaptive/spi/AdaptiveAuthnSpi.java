package org.keycloak.adaptive.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AdaptiveAuthnSpi implements Spi {
    public static final String SPI_NAME = "adaptive-authn-spi";

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
        return AdaptiveAuthnProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AdaptiveAuthnFactory.class;
    }
}
