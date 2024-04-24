package org.keycloak.adaptive.policy;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorFactory;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthnPolicyAuthenticatorFactory implements AuthenticatorFactory {
    private static final Logger logger = Logger.getLogger(AuthnPolicyAuthenticatorFactory.class);

    private static final String ALIAS_DELIMITER = " - ";
    protected static final String NO_USER_SUFFIX = "No user required";
    protected static final String USER_REQUIRED_SUFFIX = "User required";

    public static final String PROVIDER_ID = "advanced-authn-policy-authenticator";
    static final String REQUIRES_USER_CONFIG = "requires-user-config";
    private static final AuthnPolicyAuthenticator SINGLETON = new AuthnPolicyAuthenticator();

    @Override
    public AuthnPolicyAuthenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this::handleEvents);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Authentication policies";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    protected String getAlias(boolean requiresUser) {
        return new StringBuilder(getDisplayType())
                .append(ALIAS_DELIMITER)
                .append(requiresUser ? USER_REQUIRED_SUFFIX : NO_USER_SUFFIX)
                .toString();
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Evaluate enabled Authentication policies";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(REQUIRES_USER_CONFIG)
                .label(REQUIRES_USER_CONFIG)
                .helpText(REQUIRES_USER_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .add()
                .build();
    }

    protected void handleEvents(ProviderEvent event) {
        if (event instanceof RealmModel.RealmPostCreateEvent realmEvent) {
            logger.debugf("Handling RealmPostCreateEvent");
            configureAuthenticationFlows(realmEvent.getCreatedRealm());
        }
    }

    protected void configureAuthenticationFlows(RealmModel realm) {
        AuthenticationFlowModel browserFlow = realm.getBrowserFlow();

        if (browserFlow == null) {
            return;
        }

        var noAuthenticators = realm.getAuthenticationExecutionsStream(browserFlow.getId())
                .filter(Objects::nonNull)
                .filter(f -> StringUtil.isNotBlank(f.getAuthenticator()))
                .noneMatch(f -> f.getAuthenticator().equals(AuthnPolicyAuthenticatorFactory.PROVIDER_ID));

        if (noAuthenticators) {
            final var requirement = realm.getAuthenticationExecutionsStream(browserFlow.getId())
                    .filter(AuthenticationExecutionModel::isAlternative)
                    .findAny().map(f -> AuthenticationExecutionModel.Requirement.ALTERNATIVE)
                    .orElse(AuthenticationExecutionModel.Requirement.REQUIRED);

            createAuthenticatorNoUserRequired(realm, requirement);
            createAuthenticatorUserRequired(realm, requirement);
        }
    }

    protected void createAuthenticatorNoUserRequired(RealmModel realm, AuthenticationExecutionModel.Requirement requirement) {
        createAuthenticator(realm, false, requirement);
    }

    protected void createAuthenticatorUserRequired(RealmModel realm, AuthenticationExecutionModel.Requirement requirement) {
        createAuthenticator(realm, true, requirement);
    }

    protected void createAuthenticator(RealmModel realm, boolean requiresUser, AuthenticationExecutionModel.Requirement requirement) {
        final var priority = requiresUser ? 999 : -999;

        var config = new AuthenticatorConfigModel();
        config.setConfig(Map.of(REQUIRES_USER_CONFIG, Boolean.valueOf(requiresUser).toString()));
        config.setAlias(getAlias(requiresUser));
        config = realm.addAuthenticatorConfig(config);

        final AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setRequirement(requirement);
        execution.setPriority(priority);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(AuthnPolicyAuthenticatorFactory.PROVIDER_ID);
        execution.setParentFlow(realm.getBrowserFlow().getId());
        execution.setAuthenticatorConfig(config.getId());

        realm.addAuthenticatorExecution(execution);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {
    }
}
