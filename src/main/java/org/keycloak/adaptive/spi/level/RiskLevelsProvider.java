package org.keycloak.adaptive.spi.level;

import org.keycloak.provider.Provider;

import java.util.Collection;

public interface RiskLevelsProvider extends Provider {

    Collection<RiskLevel> getRiskLevels();
}
