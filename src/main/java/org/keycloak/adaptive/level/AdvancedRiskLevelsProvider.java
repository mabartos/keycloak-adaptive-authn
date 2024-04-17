package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

import java.util.Set;

public class AdvancedRiskLevelsProvider implements RiskLevelsProvider {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.2);
    static final RiskLevel MILD = new SimpleRiskLevel("MILD", 0.21, 0.4);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.41, 0.6);
    static final RiskLevel MODERATE = new SimpleRiskLevel("MODERATE", 0.61, 0.8);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.81, 1.0);

    @Override
    public Set<RiskLevel> getRiskLevels() {
        return Set.of(LOW, MILD, MEDIUM, MODERATE, HIGH);
    }
}
