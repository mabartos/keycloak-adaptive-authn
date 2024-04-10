package org.keycloak.adaptive.context.os;

import org.keycloak.Config;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.factor.UserContext;
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

public class OperatingSystemConditionFactory implements UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-os-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String OS_CONFIG = "os-config";

    private Set<Operation<DeviceContext>> rules;

    static Operation<DeviceContext> RULE_EQ = new Operation<>(DefaultOperation.EQ,
            (ua, val) -> ua.getData().getOs().startsWith(val));
    static Operation<DeviceContext> RULE_NEQ = new Operation<>(DefaultOperation.NEQ,
            (ua, val) -> !ua.getData().getOs().startsWith(val));
    static Operation<DeviceContext> RULE_ANY_OF = new Operation<>(DefaultOperation.ANY_OF,
            (ua, val) -> List.of(val.split(",")).contains(ua.getData().getOs()));
    static Operation<DeviceContext> RULE_NONE_OF = new Operation<>(DefaultOperation.NONE_OF,
            (ua, val) -> !List.of(val.split(",")).contains(ua.getData().getOs()));

    public OperatingSystemConditionFactory() {
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        final var context = session.getKeycloakSessionFactory()
                .getProviderFactory(UserContext.class, DeviceContextFactory.PROVIDER_ID);

        if (context == null) {
            throw new RuntimeException("Cannot find DeviceContext provider factory");
        }
        return new OperatingSystemCondition(session, (DeviceContext) context.create(session), rules);
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
        return "Condition matching Operating system";
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
                .name(OS_CONFIG)
                .label(OS_CONFIG)
                .helpText(OS_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .defaultValue("")
                .options(DefaultOperatingSystems.DEFAULT_OPERATING_SYSTEMS.stream().toList())
                .add()
                .build();
    }

    @Override
    public String getDisplayType() {
        return "Condition - Operating System";
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
    public Set<Operation<DeviceContext>> getRules() {
        return rules;
    }

    public List<String> getRulesTexts() {
        return getRules().stream().map(Operation::getText).toList();
    }
}

