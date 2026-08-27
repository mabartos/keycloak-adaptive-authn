package io.github.mabartos.engine.core;

import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutorFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Factory for the virtual threads remote evaluator executor.
 * <p>
 * Requires Java 21+ with {@code --enable-preview}, or Java 23+ without it.
 */
public class VTRemoteEvaluatorExecutorFactory implements RemoteEvaluatorExecutorFactory, EnvironmentDependentProviderFactory {
    public static final String PROVIDER_ID = "default-vt";

    @Override
    public RemoteEvaluatorExecutor create(KeycloakSession session) {
        return new VTRemoteEvaluatorExecutor();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        try {
            int majorVersion = Runtime.version().feature();
            if (majorVersion < 21) {
                return false;
            }
            if (majorVersion < 23) {
                try {
                    Class.forName("java.util.concurrent.StructuredTaskScope");
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
