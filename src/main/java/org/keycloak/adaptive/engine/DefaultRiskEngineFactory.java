/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.level.SimpleRiskLevelConditionFactory;
import org.keycloak.adaptive.level.SimpleRiskLevelsFactory;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.RiskEngineFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.keycloak.adaptive.ui.RiskBasedPoliciesUiTab.RISK_LEVEL_PROVIDER_CONFIG;
import static org.keycloak.authentication.AuthenticationFlow.BASIC_FLOW;

public class DefaultRiskEngineFactory implements RiskEngineFactory {
    private static final Logger logger = Logger.getLogger(DefaultRiskEngineFactory.class);

    public static final String PROVIDER_ID = "default-risk-engine";
    public static final String REQUIRES_USER_CONFIG = "requiresUserConfig";

    public static final Duration DEFAULT_EVALUATOR_TIMEOUT = Duration.ofMillis(2500L);
    public static final int DEFAULT_EVALUATOR_RETRIES = 3;

    public static final String EVALUATOR_TIMEOUT_CONFIG = "riskEvaluatorTimeoutConfig";
    public static final String EVALUATOR_RETRIES_CONFIG = "riskEvaluatorRetriesConfig";

    protected static final String DEFAULT_RISK_BASED_POLICY_ALIAS = "POLICY - Risk-based";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new DefaultRiskEngine(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this::handleEvents);
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
        return "Risk evaluator";
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

    protected void handleEvents(ProviderEvent event) {
        if (event instanceof RealmModel.RealmPostCreateEvent realmEvent) {
            logger.debugf("Handling RealmPostCreateEvent");
            configureAuthenticationFlows(realmEvent.getKeycloakSession(), realmEvent.getCreatedRealm());
        }
    }

    protected void configureAuthenticationFlows(KeycloakSession session, RealmModel realm) {
        /*var factory = session.getKeycloakSessionFactory().getProviderFactory(AuthnPolicyProvider.class);
        if (factory == null) {
            logger.debugf("Cannot find AuthnPolicyProviderFactory");
            return;
        }

        var authnPolicyProvider = ((AuthnPolicyProviderFactory) factory).create(session, realm);
        if (authnPolicyProvider == null) {
            logger.debugf("Cannot find any AuthnPolicyProvider");
            return;
        }

        authnPolicyProvider.getOrCreateParentPolicy();

        if (!Boolean.getBoolean("createRiskBasedPolicy")) {
            return;
        }

        final var existing = Optional.ofNullable(realm.getFlowByAlias(DEFAULT_RISK_BASED_POLICY_ALIAS));
        if (existing.isPresent()) {
            logger.warnf("Default policy '%s' already exists", DEFAULT_RISK_BASED_POLICY_ALIAS);
            return;
        }

        final var riskLevelProviderId = Optional.ofNullable(realm.getAttribute(RISK_LEVEL_PROVIDER_CONFIG))
                .filter(StringUtil::isNotBlank)
                .orElse(SimpleRiskLevelsFactory.PROVIDER_ID);

        var riskLevelsProvider = session.getProvider(RiskLevelsProvider.class, riskLevelProviderId);
        if (riskLevelsProvider == null) {
            logger.debugf("Cannot find RiskLevelsProvider '%s'", riskLevelProviderId);
            return;
        }

        AuthenticationFlowModel policy = new AuthenticationFlowModel();
        policy.setAlias("Evaluate risk score - no user");
        policy.setDescription("Evaluate risk score - no user");
        policy.setProviderId(BASIC_FLOW);
        policy.setTopLevel(false);
        policy.setBuiltIn(false);
        policy = authnPolicyProvider.addPolicy(policy);

        // Risk Evaluator - no user required
        var configModel = new AuthenticatorConfigModel();
        configModel.setConfig(Map.of(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG, Boolean.FALSE.toString()));
        configModel.setAlias("Risk evaluator - no user required");
        configModel = realm.addAuthenticatorConfig(configModel);

        var execution = new AuthenticationExecutionModel();
        execution.setParentFlow(policy.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(DefaultRiskEngineFactory.PROVIDER_ID);
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        // Parent Risk-based Policy
        policy = new AuthenticationFlowModel();
        policy.setAlias(DEFAULT_RISK_BASED_POLICY_ALIAS);
        policy.setDescription("Policy leveraging risk-based authentication");
        policy.setProviderId(BASIC_FLOW);
        policy.setTopLevel(false);
        policy.setBuiltIn(false);
        policy = authnPolicyProvider.addPolicy(policy);

        // Evaluate risk provider
        configModel = new AuthenticatorConfigModel();
        configModel.setConfig(Map.of(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG, Boolean.TRUE.toString()));
        configModel.setAlias("Risk evaluator - requires user");
        configModel = realm.addAuthenticatorConfig(configModel);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(policy.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(DefaultRiskEngineFactory.PROVIDER_ID);
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);

        // Levels
        AtomicInteger priority = new AtomicInteger(30);
        for (var level : riskLevelsProvider.getRiskLevels()) {
            // Conditional flow for level
            AuthenticationFlowModel levelFlow = new AuthenticationFlowModel();
            levelFlow.setTopLevel(false);
            levelFlow.setBuiltIn(false);
            levelFlow.setAlias(level.getName() + " Risk");
            levelFlow.setDescription(level.getName() + " Risk");
            levelFlow.setProviderId(BASIC_FLOW);
            levelFlow = realm.addAuthenticationFlow(levelFlow);
            var levelFlowExecution = new AuthenticationExecutionModel();
            levelFlowExecution.setParentFlow(policy.getId());
            levelFlowExecution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            levelFlowExecution.setFlowId(levelFlow.getId());
            levelFlowExecution.setPriority(priority.getAndAdd(10));
            levelFlowExecution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(levelFlowExecution);

            // Condition for level
            configModel = new AuthenticatorConfigModel();
            configModel.setConfig(Map.of(SimpleRiskLevelConditionFactory.LEVEL_CONFIG, level.getName()));
            configModel.setAlias(level.getName());
            configModel = realm.addAuthenticatorConfig(configModel);

            var levelCondition = new AuthenticationExecutionModel();
            levelCondition.setParentFlow(levelFlow.getId());
            levelCondition.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            levelCondition.setAuthenticator(SimpleRiskLevelConditionFactory.PROVIDER_ID);
            levelCondition.setPriority(priority.getAndAdd(5));
            levelCondition.setAuthenticatorFlow(false);
            levelCondition.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(levelCondition);
        }*/
    }
}
