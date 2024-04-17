package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.policy.DefaultOperation;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.OperationsBuilder;
import org.keycloak.adaptive.spi.condition.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class OperatingSystemConditionFactory extends UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-os-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String OS_CONFIG = "os-config";

    public OperatingSystemConditionFactory() {
    }

    @Override
    public String getDisplayType() {
        return "Condition - Operating System";
    }

    @Override
    public String getHelpText() {
        return "Condition matching Operating system";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new OperatingSystemCondition(session, getRules());
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

    public static boolean isOs(DeviceContext context, String os) {
        return Optional.ofNullable(context.getData())
                .map(DeviceRepresentation::getOs)
                .map(f -> f.startsWith(os))
                .orElse(false);
    }

    @Override
    public Set<Operation<DeviceContext>> initRules() {
        return OperationsBuilder.builder(DeviceContext.class)
                .operation()
                    .operationKey(DefaultOperation.EQ)
                    .condition(OperatingSystemConditionFactory::isOs)
                .add()
                .operation()
                    .operationKey(DefaultOperation.NEQ)
                    .condition((dev, val) -> !isOs(dev, val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.ANY_OF)
                    .condition((dev, val) -> List.of(val.split(",")).contains(dev.getData().getOs()))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NONE_OF)
                    .condition((dev, val) -> !List.of(val.split(",")).contains(dev.getData().getOs()))
                .add()
                .build();
    }
}