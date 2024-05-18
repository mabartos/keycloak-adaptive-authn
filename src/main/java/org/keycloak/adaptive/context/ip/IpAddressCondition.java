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

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DefaultDeviceContextFactory;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.List;

public class IpAddressCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final List<Operation<DeviceContext>> rules;

    public IpAddressCondition(KeycloakSession session, List<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null) {
            var operation = authConfig.getConfig().get(IpAddressConditionFactory.OPERATION_CONFIG);
            var ip = authConfig.getConfig().get(IpAddressConditionFactory.IP_ADDRESS_CONFIG);

            if (StringUtil.isBlank(operation) || StringUtil.isBlank(ip)) return false;
            return rules.stream()
                    .filter(f -> f.getText().equals(operation))
                    .allMatch(f -> f.match(deviceContext, ip));
        }
        return false;
    }
}
