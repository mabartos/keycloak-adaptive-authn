package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.Provider;

import java.util.Set;

public interface RiskFactorEvaluator extends Provider {

    Double getRiskValue();

    Set<UserContext<?>> getUserContexts();

    void evaluate();

    default void close() {
    }
}
