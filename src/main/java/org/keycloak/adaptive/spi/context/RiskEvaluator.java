package org.keycloak.adaptive.spi.context;

import org.keycloak.adaptive.level.Weight;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.util.Collections;
import java.util.Set;

public interface RiskEvaluator extends Provider {

    Double getRiskValue();

    default double getWeight() {
        return Weight.NORMAL;
    }

    default Set<UserContext<?>> getUserContexts() {
        return Collections.emptySet();
    }

    void evaluate();

    default void close() {
    }
}
