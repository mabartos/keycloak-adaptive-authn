package org.keycloak.adaptive.level;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Optional;

public abstract class AbstractRiskLevelConditionFactory implements ConditionalAuthenticatorFactory {
    public static final String LEVEL_CONFIG = "level-config";
    private static ConditionalAuthenticator RISK_LEVELS_CONDITION;

    private RiskLevelsFactory riskLevelsfactory;

    public abstract String getRiskLevelProviderId();

    @Override
    public ConditionalAuthenticator getSingleton() {
        if (RISK_LEVELS_CONDITION == null) {
            RISK_LEVELS_CONDITION = new RiskLevelCondition(riskLevelsfactory.getSingleton());
        }
        return RISK_LEVELS_CONDITION;
    }

    @Override
    public String getDisplayType() {
        return String.format("Condition - Risk Level (%s)", riskLevelsfactory.getName());
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return riskLevelsfactory.getHelpText();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(LEVEL_CONFIG)
                .options(riskLevelsfactory.getSingleton().getRiskLevels().stream().map(RiskLevel::getName).toList())
                .label(LEVEL_CONFIG)
                .helpText(LEVEL_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .build();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.riskLevelsfactory = Optional.ofNullable(factory.getProviderFactory(RiskLevelsProvider.class, getRiskLevelProviderId()))
                .filter(f -> f instanceof RiskLevelsFactory)
                .map(f -> (RiskLevelsFactory) f)
                .orElseThrow(() -> new IllegalStateException(String.format("Cannot find Risk Level Factory '%s'", getRiskLevelProviderId())));
    }

    @Override
    public void close() {

    }
}
