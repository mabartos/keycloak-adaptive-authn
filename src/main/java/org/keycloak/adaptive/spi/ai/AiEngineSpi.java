package org.keycloak.adaptive.spi.ai;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AiEngineSpi implements Spi {
    public static final String NAME = "ai-engine-spi";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AiNlpEngine.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AiEngineFactory.class;
    }
}
