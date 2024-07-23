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
package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.spi.condition.DefaultOperation;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.OperationsBuilder;
import org.keycloak.adaptive.spi.condition.UserContextConditionFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Optional;

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
        return new PhoneCondition(session, getOperations());
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
    public List<Operation<DeviceContext>> initOperations() {
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

