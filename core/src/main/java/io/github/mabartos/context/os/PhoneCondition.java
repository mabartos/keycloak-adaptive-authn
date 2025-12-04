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

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceContext;
import io.github.mabartos.context.device.DefaultDeviceContextFactory;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;

/**
 * Condition for checking whether the accessing device is a phone
 */
public class PhoneCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final List<Operation<DeviceContext>> rules;

    public PhoneCondition(KeycloakSession session, List<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = UserContexts.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null) {
            boolean isMobile = Boolean.parseBoolean(authConfig.getConfig().get(PhoneConditionFactory.IS_MOBILE_CONF));
            return rules.stream().allMatch(f -> f.match(deviceContext, Boolean.toString(isMobile)));
        }
        return false;
    }
}
