package org.keycloak.adaptive.level;

import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;

public class SimpleRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "simple-risk-levels";
    private static final RiskLevelsProvider SINGLETON = new SimpleRiskLevelsProvider();

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
        return "Simple";
    }

    @Override
    public String getHelpText() {
        return "Risk levels - 3 levels (Low, Medium, High)";
    }
}
