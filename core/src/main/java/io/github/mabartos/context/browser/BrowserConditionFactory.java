/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.context.browser;

import io.github.mabartos.context.device.DeviceContext;
import io.github.mabartos.spi.condition.DefaultOperation;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.OperationsBuilder;
import io.github.mabartos.spi.condition.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.List;

public class BrowserConditionFactory extends UserContextConditionFactory<DeviceContext> {
    public static final String PROVIDER_ID = "conditional-user-agent-authenticator";
    public static final String OPERATION_CONFIG = "operation";
    public static final String BROWSER_CONFIG = "browser-config";

    public BrowserConditionFactory() {
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new BrowserCondition(session, getOperations());
    }

    @Override
    public List<Operation<DeviceContext>> initOperations() {
        return OperationsBuilder.builder(DeviceContext.class)
                .operation()
                    .operationKey(DefaultOperation.EQ)
                    .condition((dev, val) -> dev.getData().map(DeviceRepresentation::getBrowser).filter(f -> f.startsWith(val)).isPresent())
                .add()
                .operation()
                    .operationKey(DefaultOperation.NEQ)
                    .condition((dev, val) -> dev.getData().map(DeviceRepresentation::getBrowser).filter(f -> f.startsWith(val)).isEmpty())
                .add()
                .operation()
                    .operationKey(DefaultOperation.ANY_OF)
                    .condition((dev, val) -> List.of(val.split(",")).contains(dev.getData().map(DeviceRepresentation::getBrowser).orElse("<unknown>")))
                .add()
                .operation()
                    .operationKey(DefaultOperation.NONE_OF)
                    .condition((dev, val) -> !List.of(val.split(",")).contains(dev.getData().map(DeviceRepresentation::getBrowser).orElse("<unknown>")))
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
                .options(getOperationsTexts())
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
}
