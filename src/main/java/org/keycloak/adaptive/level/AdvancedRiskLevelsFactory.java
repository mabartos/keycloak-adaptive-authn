package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

public class AdvancedRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "advanced-risk-levels";
    private static final RiskLevelsProvider SINGLETON = new AdvancedRiskLevelsProvider();

    @Override
    public RiskLevelsProvider getSingleton() {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Advanced";
    }

    @Override
    public String getHelpText() {
        return "Risk levels - 5 levels (Low, Mild, Medium, Moderate, High)";
    }
}
