package org.keycloak.adaptive.level;

public class SimpleRiskLevelConditionFactory extends AbstractRiskLevelConditionFactory {
    public static final String PROVIDER_ID = "simple-risk-levels-condition";

    @Override
    public String getRiskLevelProviderId() {
        return SimpleRiskLevelsFactory.PROVIDER_ID;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
