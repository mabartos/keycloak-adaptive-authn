package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

import java.util.Collections;
import java.util.Set;

public interface RiskEvaluator extends Provider {

    Double getRiskValue();

    double getWeight();

    default Set<UserContext<?>> getUserContexts() {
        return Collections.emptySet();
    }

    void evaluate();

    default void close() {
    }
}
