package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.spi.policy.DefaultOperation;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.OperationsBuilder;
import org.keycloak.adaptive.spi.policy.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PhoneConditionFactory extends UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-mobile-authenticator";
    public static final String IS_MOBILE_CONF = "is-mobile";

    public PhoneConditionFactory() {
    }

    @Override
    public String getDisplayType() {
        return "Condition - Phone";
    }

    @Override
    public String getHelpText() {
        return "Condition whether device is phone or not.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PhoneCondition(session, getRules());
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
    public Set<Operation<DeviceContext>> initRules() {
        return OperationsBuilder.builder(DeviceContext.class)
                .operation()
                .operationKey(DefaultOperation.IS)
                .condition(this::isMobilePhone)
                .add()
                .build();
    }

    protected boolean isMobilePhone(DeviceContext context, String value) {
        return Optional.ofNullable(context.getData())
                .map(f -> Boolean.valueOf(f.isMobile()).toString())
                .map(f -> f.equals(value))
                .orElse(false);
    }
}

