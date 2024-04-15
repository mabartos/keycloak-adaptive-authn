package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.spi.policy.DefaultOperation;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.OperationsBuilder;
import org.keycloak.adaptive.spi.policy.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
                    .condition(IpAddressConditionFactory::isInRange)
                .add()
                .operation()
                    .operationKey(DefaultOperation.NOT_IN_RANGE)
                    .condition(IpAddressConditionFactory::isInRange)
                .add()
                .build();
    }

    protected static boolean isInRange(DeviceContext context, String value) {
        if (StringUtil.isBlank(value)) throw new IllegalArgumentException("Cannot parse IP Address");

        return Arrays.stream(value.split(","))
                .filter(f -> f.contains("-"))
                .anyMatch(f -> manageRange(context, f));
    }

    private static boolean manageRange(DeviceContext context, String ipAddress) {
        var items = ipAddress.split("-");
        if (items.length != 2) {
            throw new IllegalArgumentException("Invalid IP Address range format");
        }

        try {
            var start = new IPAddressString(items[0]).getAddress();
            var end = new IPAddressString(items[1]).getAddress();
            var ipRange = start.spanWithRange(end);

            var deviceIp = Optional.ofNullable(context.getData())
                    .map(DeviceRepresentation::getIpAddress)
                    .filter(StringUtil::isNotBlank);

            if (deviceIp.isEmpty()) throw new IllegalArgumentException("Cannot obtain IP Address");

            return ipRange.contains(new IPAddressString(deviceIp.get()).getAddress());
        } catch (IncompatibleAddressException e) {
            throw new IllegalArgumentException("Cannot parse IP Address", e);
        }
    }

    static Optional<IPAddress> getIpAddress(String ipAddress) {
        try {
            return Optional.ofNullable(new IPAddressString(ipAddress).getAddress());
        } catch (IncompatibleAddressException e) {
            return Optional.empty();
        }
    }
}