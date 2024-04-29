package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

import java.util.Collections;
import java.util.Set;

public interface RiskEvaluator extends Provider {

    Double getRiskValue();

    double getWeight();

    boolean requiresUser();

    void evaluate();

    default boolean isEnabled() {
        return true;
    }

    default void close() {
    }
}
