package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.policy.DefaultOperation;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.OperationsBuilder;
import org.keycloak.adaptive.spi.policy.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Set;

public class BrowserConditionFactory extends UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-user-agent-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String BROWSER_CONFIG = "browser-config";

    public BrowserConditionFactory() {
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new BrowserCondition(session, createContext(session, DeviceContextFactory.PROVIDER_ID), getRules());
    }

    @Override
    public Set<Operation<DeviceContext>> initRules() {
        return OperationsBuilder.builder(DeviceContext.class)
                .operation()
                    .operationKey(DefaultOperation.EQ)
                    .condition((dev, val) -> dev.getData().getBrowser().startsWith(val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NEQ)
                    .condition((dev, val) -> !dev.getData().getBrowser().startsWith(val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.ANY_OF)
                    .condition((dev, val) -> List.of(val.split(",")).contains(dev.getData().getBrowser()))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NONE_OF)
                    .condition((dev, val) -> !List.of(val.split(",")).contains(dev.getData().getBrowser()))
                .add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public String getHelpText() {
        return "Condition matching browser";
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
                .name(BROWSER_CONFIG)
                .label(BROWSER_CONFIG)
                .helpText(BROWSER_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .defaultValue("")
                .options(DefaultBrowsers.DEFAULT_BROWSERS.stream().toList())
                .add()
                .build();
    }

    @Override
    public String getDisplayType() {
        return "Condition - Browser";
    }

    public List<String> getRulesTexts() {
        return getRules().stream().map(Operation::getText).toList();
    }
}
