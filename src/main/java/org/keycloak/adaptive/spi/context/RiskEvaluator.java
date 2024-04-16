package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

import java.util.Set;

public interface RiskEvaluator extends Provider {

    Double getRiskValue();

    Set<UserContext<?>> getUserContexts();

    void evaluate();

    default void close() {
    }
}
