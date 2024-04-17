package org.keycloak.adaptive.level;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.level.RiskLevel;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Optional;

import static org.keycloak.adaptive.ui.RiskBasedPoliciesUiTab.RISK_LEVEL_PROVIDER_CONFIG;

public class RiskLevelConditionFactory implements ConditionalAuthenticatorFactory {
    public static final String PROVIDER_ID = "risk-level-condition-factory";
    public static final String LEVEL_CONFIG = "level-config";

    private RiskLevelsProvider riskLevelsProvider;

    @Override
    public Authenticator create(KeycloakSession session) {
        var riskLevelProviderId = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RISK_LEVEL_PROVIDER_CONFIG))
                .orElse(SimpleRiskLevelsFactory.PROVIDER_ID);

        this.riskLevelsProvider = session.getProvider(RiskLevelsProvider.class, riskLevelProviderId);
        return new RiskLevelCondition(riskLevelsProvider);
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return null;
    }

    @Override
    public String getDisplayType() {
        return "Condition - Risk Level";
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
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(LEVEL_CONFIG)
                .options(riskLevelsProvider.getRiskLevels().stream().map(RiskLevel::getName).toList())
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
        riskLevelsProvider = factory.getProviderFactory(RiskLevelsProvider.class, SimpleRiskLevelsFactory.PROVIDER_ID).create(null);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
