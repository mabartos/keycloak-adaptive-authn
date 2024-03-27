package org.keycloak.adaptive.context.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.DefaultOperation;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Set;

public class UserAgentConditionFactory implements UserContextConditionFactory<UserAgentContext> {
    public static final String PROVIDER_ID = "conditional-user-agent-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String USER_AGENT_CONFIG = "user-agent-config";

    private Set<Operation<UserAgentContext>> rules;

    static Operation<UserAgentContext> RULE_EQ = new Operation<>(DefaultOperation.EQ,
            (ua, val) -> ua.getData().getName().equals(val));
    static Operation<UserAgentContext> RULE_NEQ = new Operation<>(DefaultOperation.NEQ,
            (ua, val) -> ua.getData().getName().equals(val));
    static Operation<UserAgentContext> RULE_ANY_OF = new Operation<>(DefaultOperation.ANY_OF,
            (ua, val) -> List.of(val.split(",")).contains(ua.getData().getName()));
    static Operation<UserAgentContext> RULE_NONE_OF = new Operation<>(DefaultOperation.NONE_OF,
            (ua, val) -> !List.of(val.split(",")).contains(ua.getData().getName()));

    public UserAgentConditionFactory() {
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return new UserAgentCondition(session, rules);
    }

    @Override
    public void init(Config.Scope config) {
        this.rules = Set.of(
                RULE_EQ,
                RULE_NEQ,
                RULE_ANY_OF,
                RULE_NONE_OF
        );
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
    public String getHelpText() {
        return "Condition matching user agent";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(OPERATION_CONFIG)
                .options(getRulesTexts())
                .label(OPERATION_CONFIG)
                .helpText(OPERATION_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .property()
                .name(USER_AGENT_CONFIG)
                .label(USER_AGENT_CONFIG)
                .helpText(USER_AGENT_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .defaultValue("")
                .options(DefaultUserAgents.KNOWN_AGENTS.stream().map(UserAgent::getName).toList())
                .add()
                .build();
    }

    @Override
    public String getDisplayType() {
        return "Condition - User Agent";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return null;
    }

    @Override
    public Set<Operation<UserAgentContext>> getRules() {
        return rules;
    }

    public List<String> getRulesTexts() {
        return getRules().stream().map(Operation::getText).toList();
    }
}
