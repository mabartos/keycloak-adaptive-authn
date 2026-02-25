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
package io.github.mabartos.context.os;

import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.spi.condition.DefaultOperation;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.OperationsBuilder;
import io.github.mabartos.spi.condition.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.List;

public class OperatingSystemConditionFactory extends UserContextConditionFactory<DeviceRepresentationContext> {
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
        return new OperatingSystemCondition(session, getOperations());
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
                .name(OS_CONFIG)
                .label(OS_CONFIG)
                .helpText(OS_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .defaultValue("")
                .options(DefaultOperatingSystems.DEFAULT_OPERATING_SYSTEMS.stream().toList())
                .add()
                .build();
    }

    public static boolean isOs(RealmModel realm, DeviceRepresentationContext context, String os) {
        return context.getData(realm)
                .map(DeviceRepresentation::getOs)
                .map(f -> f.startsWith(os))
                .orElse(false);
    }

    protected static boolean isOsSpecified(RealmModel realm, DeviceRepresentationContext device, List<String> specifiedSystems) {
        return specifiedSystems.contains(device.getData(realm).map(DeviceRepresentation::getOs).orElse("<unknown>"));
    }

    @Override
    public List<Operation<DeviceRepresentationContext>> initOperations() {
        return OperationsBuilder.builder(DeviceRepresentationContext.class, ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .operation()
                    .operationKey(DefaultOperation.EQ)
                    .condition(OperatingSystemConditionFactory::isOs)
                .add()
                .operation()
                    .operationKey(DefaultOperation.NEQ)
                    .condition((realm, dev, val) -> !isOs(realm, dev, val))
                .add()
                .operation()
                    .operationKey(DefaultOperation.ANY_OF)
                    .multiValuedCondition(OperatingSystemConditionFactory::isOsSpecified)
                .add()
                .operation()
                    .operationKey(DefaultOperation.NONE_OF)
                    .multiValuedCondition((realm, dev, list) -> !isOsSpecified(realm, dev, list))
                .add()
                .build();
    }
}