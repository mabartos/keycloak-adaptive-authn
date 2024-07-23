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
        return new IpAddressCondition(session, getOperations());
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
                .name(IP_ADDRESS_CONFIG)
                .label(IP_ADDRESS_CONFIG)
                .helpText(IP_ADDRESS_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("")
                .add()
                .build();
    }

    @Override
    public List<Operation<DeviceContext>> initOperations() {
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