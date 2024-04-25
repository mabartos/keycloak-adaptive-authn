package org.keycloak.adaptive.policy;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.engine.DefaultRiskEngineFactory;
import org.keycloak.adaptive.level.RiskLevelConditionFactory;
import org.keycloak.adaptive.level.SimpleRiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.utils.StringUtil;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.keycloak.adaptive.ui.RiskBasedPoliciesUiTab.RISK_LEVEL_PROVIDER_CONFIG;
import static org.keycloak.authentication.AuthenticationFlow.BASIC_FLOW;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    private static final Logger logger = Logger.getLogger(DefaultAuthnPolicyFactory.class);
    public static final String DEFAULT_AUTHN_POLICIES_FLOW_ALIAS = "Authentication policies - PARENT";

    public static final String PROVIDER_ID = "default";
    protected static final String DEFAULT_RISK_BASED_POLICY_ALIAS = "POLICY - Risk-based";

    @Override
    public AuthnPolicyProvider create(KeycloakSession session) {
        return new DefaultAuthnPolicyProvider(session);
    }

    @Override
    public AuthnPolicyProvider create(KeycloakSession session, RealmModel realm) {
        return new DefaultAuthnPolicyProvider(session, realm);
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

    protected void handleEvents(ProviderEvent event) {
        if (event instanceof RealmModel.RealmPostCreateEvent realmEvent) {
            logger.debugf("Handling RealmPostCreateEvent");
            configureAuthenticationFlows(realmEvent.getKeycloakSession(), realmEvent.getCreatedRealm());
        }
    }

    static AuthenticationFlowModel createParentFlow(RealmModel realm) {
        var parent = new AuthenticationFlowModel();
        parent.setAlias(DEFAULT_AUTHN_POLICIES_FLOW_ALIAS);
        parent.setDescription("Parent Authentication Policy");
        parent.setProviderId(BASIC_FLOW);
        parent.setTopLevel(true);
        parent.setBuiltIn(false);
        return realm.addAuthenticationFlow(parent);
    }

    protected void configureAuthenticationFlows(KeycloakSession session, RealmModel realm) {
        var factory = session.getKeycloakSessionFactory().getProviderFactory(AuthnPolicyProvider.class);
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

        // Parent Risk-based Policy
        AuthenticationFlowModel policy = new AuthenticationFlowModel();
        policy.setAlias(DEFAULT_RISK_BASED_POLICY_ALIAS);
        policy.setDescription("Policy leveraging risk-based authentication");
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

        // Conditional policy
        AuthenticationFlowModel conditionalPolicy = new AuthenticationFlowModel();
        conditionalPolicy.setAlias("Risk-based");
        conditionalPolicy.setDescription("Policy leveraging risk-based authentication");
        conditionalPolicy.setProviderId(BASIC_FLOW);
        conditionalPolicy.setTopLevel(false);
        conditionalPolicy.setBuiltIn(false);
        conditionalPolicy = realm.addAuthenticationFlow(conditionalPolicy);

        // Conditional policy execution
        var conditionalPolicyExecution = new AuthenticationExecutionModel();
        conditionalPolicyExecution.setParentFlow(policy.getId());
        conditionalPolicyExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED); // Admins needs to explicitly enable it
        conditionalPolicyExecution.setFlowId(conditionalPolicy.getId());
        conditionalPolicyExecution.setPriority(0);
        conditionalPolicyExecution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(conditionalPolicyExecution);

        // Evaluate risk provider
        configModel = new AuthenticatorConfigModel();
        configModel.setConfig(Map.of(DefaultRiskEngineFactory.REQUIRES_USER_CONFIG, Boolean.TRUE.toString()));
        configModel.setAlias("Risk evaluator - requires user");
        configModel = realm.addAuthenticatorConfig(configModel);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalPolicy.getId());
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
            levelFlowExecution.setParentFlow(conditionalPolicy.getId());
            levelFlowExecution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
            levelFlowExecution.setFlowId(levelFlow.getId());
            levelFlowExecution.setPriority(priority.getAndAdd(10));
            levelFlowExecution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(levelFlowExecution);

            // Condition for level
            configModel = new AuthenticatorConfigModel();
            configModel.setConfig(Map.of(RiskLevelConditionFactory.LEVEL_CONFIG, level.getName()));
            configModel.setAlias(level.getName());
            configModel = realm.addAuthenticatorConfig(configModel);

            var levelCondition = new AuthenticationExecutionModel();
            levelCondition.setParentFlow(levelFlow.getId());
            levelCondition.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            levelCondition.setAuthenticator(RiskLevelConditionFactory.PROVIDER_ID);
            levelCondition.setPriority(priority.getAndAdd(5));
            levelCondition.setAuthenticatorFlow(false);
            levelCondition.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(levelCondition);
        }
    }
}
