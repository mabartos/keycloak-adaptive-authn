package org.keycloak.adaptive.context.ip;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.policy.DefaultOperation;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.OperationsBuilder;
import org.keycloak.adaptive.spi.condition.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Set;

public class IpAddressConditionFactory extends UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-ip-address-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String IP_ADDRESS_CONFIG = "ip-address-config";

    public IpAddressConditionFactory() {
    }

    @Override
    public String getDisplayType() {
        return "Condition - IP Address";
    }

    @Override
    public String getHelpText() {
        return "Condition matching IP Addresses";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IpAddressCondition(session, getRules());
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
                .name(IP_ADDRESS_CONFIG)
                .label(IP_ADDRESS_CONFIG)
                .helpText(IP_ADDRESS_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
                .add()
                .build();
    }

    @Override
    public Set<Operation<DeviceContext>> initRules() {
        return OperationsBuilder.builder(DeviceContext.class)
                .operation()
                    .operationKey(DefaultOperation.EQ)
                    .condition((dev, val) -> dev.getData().getIpAddress().startsWith(val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NEQ)
                    .condition((dev, val) -> !dev.getData().getIpAddress().startsWith(val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.ANY_OF)
                    .condition((dev, val) -> List.of(val.split(",")).contains(dev.getData().getIpAddress()))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NONE_OF)
                    .condition((dev, val) -> !List.of(val.split(",")).contains(dev.getData().getIpAddress()))
                .add()
                .operation()
                    .operationKey(DefaultOperation.IN_RANGE)
                    .condition(IpAddressUtils::isInRange)
                .add()
                .operation()
                    .operationKey(DefaultOperation.NOT_IN_RANGE)
                    .condition(IpAddressUtils::isInRange)
                .add()
                .build();
    }
}