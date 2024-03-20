package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.Provider;

import java.util.Collection;

public interface RiskFactorEvaluator<T> extends Provider {

    Double getRiskValue();

    Collection<T> getUserContexts();

    void evaluate();

    default void close() {
    }
}
