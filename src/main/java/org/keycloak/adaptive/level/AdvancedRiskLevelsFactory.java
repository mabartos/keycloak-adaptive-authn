package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.models.KeycloakSession;

public class AdvancedRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "advanced-risk-levels";
    private static final RiskLevelsProvider SINGLETON = new AdvancedRiskLevelsProvider();

    @Override
    public RiskLevelsProvider create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Risk levels - 5 levels (Low, Mild, Medium, Moderate, High)";
    }
}
