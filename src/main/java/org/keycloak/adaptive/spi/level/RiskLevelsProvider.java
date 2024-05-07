package org.keycloak.adaptive.spi.level;

import org.keycloak.provider.Provider;

import java.util.List;

public interface RiskLevelsProvider extends Provider {

    List<RiskLevel> getRiskLevels();

    @Override
    default void close() {
    }
}
