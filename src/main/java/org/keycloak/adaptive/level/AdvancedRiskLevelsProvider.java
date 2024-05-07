package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

import java.util.List;

public class AdvancedRiskLevelsProvider implements RiskLevelsProvider {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.2);
    static final RiskLevel MILD = new SimpleRiskLevel("MILD", 0.2, 0.4);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.4, 0.6);
    static final RiskLevel MODERATE = new SimpleRiskLevel("MODERATE", 0.6, 0.8);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.8, 1.0);

    @Override
    public List<RiskLevel> getRiskLevels() {
        return List.of(LOW, MILD, MEDIUM, MODERATE, HIGH);
    }
}
