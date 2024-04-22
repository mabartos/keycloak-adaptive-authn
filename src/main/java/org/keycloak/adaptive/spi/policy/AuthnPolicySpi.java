package org.keycloak.adaptive.spi.policy;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AuthnPolicySpi implements Spi {
    public static final String SPI_NAME = "authentication-policy-spi";

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
        return AuthnPolicyProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AuthnPolicyProviderFactory.class;
    }
}
