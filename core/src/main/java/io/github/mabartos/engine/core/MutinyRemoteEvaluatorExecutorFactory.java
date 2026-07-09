package io.github.mabartos.engine.core;

import io.github.mabartos.spi.engine.RemoteEvaluatorExecutor;
import io.github.mabartos.spi.engine.RemoteEvaluatorExecutorFactory;
import org.keycloak.models.KeycloakSession;

public class MutinyRemoteEvaluatorExecutorFactory implements RemoteEvaluatorExecutorFactory {
    public static final String PROVIDER_ID = "default-mutiny";

    @Override
    public RemoteEvaluatorExecutor create(KeycloakSession session) {
        return new MutinyRemoteEvaluatorExecutor(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 5;
    }
}
