package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

import java.util.Collection;
import java.util.Set;

public class SimpleRiskLevelsProvider implements RiskLevelsProvider {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.3);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.31, 0.75);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.76, 1.0);

    @Override
    public Collection<RiskLevel> getRiskLevels() {
        return Set.of(LOW, MEDIUM, HIGH);
    }

    @Override
    public void close() {

    }
}
