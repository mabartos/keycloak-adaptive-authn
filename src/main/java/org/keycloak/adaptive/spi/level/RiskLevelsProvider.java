package org.keycloak.adaptive.spi.level;

import org.keycloak.provider.Provider;

import java.util.Collection;
import java.util.Set;

public interface RiskLevelsProvider extends Provider {

    Set<RiskLevel> getRiskLevels();

    @Override
    default void close() {
    }
}
