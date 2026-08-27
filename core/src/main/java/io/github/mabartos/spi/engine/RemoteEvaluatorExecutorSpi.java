package io.github.mabartos.spi.engine;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for remote evaluator execution strategies (virtual threads, Mutiny, etc.)
 */
public class RemoteEvaluatorExecutorSpi implements Spi {
    public static final String SPI_NAME = "remote-evaluator-executor";

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
        return RemoteEvaluatorExecutor.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RemoteEvaluatorExecutorFactory.class;
    }
}
