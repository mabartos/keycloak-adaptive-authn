package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.RiskEngineFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.time.Duration;
import java.util.List;

public class SynchronousRiskEngineFactory implements RiskEngineFactory {
    public static final String PROVIDER_ID = "sync-risk-engine";
    public static final String REQUIRES_USER_CONFIG = "requiresUserConfig";

    public static final Duration DEFAULT_EVALUATOR_TIMEOUT = Duration.ofMillis(2500L);
    public static final int DEFAULT_EVALUATOR_RETRIES = 3;

    public static final String EVALUATOR_TIMEOUT_CONFIG = "riskEvaluatorTimeoutConfig";
    public static final String EVALUATOR_RETRIES_CONFIG = "riskEvaluatorRetriesConfig";

    protected static final String DEFAULT_RISK_BASED_POLICY_ALIAS = "POLICY - Risk-based";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new SynchronousRiskEngine(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Risk evaluator - synchronous";
    }

    @Override
    public String getReferenceCategory() {
        return null;
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
        return "Evaluate probability of improper authentication";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(REQUIRES_USER_CONFIG)
                .label(REQUIRES_USER_CONFIG)
                .helpText(REQUIRES_USER_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
    }
}
