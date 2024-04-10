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
import java.util.Optional;
import java.util.Set;

public class PhoneConditionFactory implements UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-mobile-authenticator";
    public static final String IS_MOBILE_CONF = "is-mobile";

    private Set<Operation<DeviceContext>> rules;

    static Operation<DeviceContext> RULE_EQ = new Operation<>(DefaultOperation.IS,
            (ua, val) -> Optional.ofNullable(ua.getData())
                    .map(f -> Boolean.valueOf(f.isMobile()).toString())
                    .map(f -> f.equals(val))
                    .orElse(false));

    public PhoneConditionFactory() {
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
        this.rules = Set.of(RULE_EQ);
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
        return "Condition whether device is phone or not.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(IS_MOBILE_CONF)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Is phone")
                .helpText("Check if device should be phone")
                .add()
                .build();
    }

    @Override
    public String getDisplayType() {
        return "Condition - Phone";
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
}

