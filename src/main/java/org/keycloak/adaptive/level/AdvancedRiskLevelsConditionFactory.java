package org.keycloak.adaptive.level;

public class AdvancedRiskLevelsConditionFactory extends AbstractRiskLevelConditionFactory {
    public static final String PROVIDER_ID = "advanced-risk-levels-condition";

    @Override
    public String getRiskLevelProviderId() {
        return AdvancedRiskLevelsFactory.PROVIDER_ID;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
