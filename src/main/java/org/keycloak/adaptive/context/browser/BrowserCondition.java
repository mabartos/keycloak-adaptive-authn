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
package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.device.DefaultDeviceContextFactory;
import org.keycloak.adaptive.context.device.DeviceContext;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Optional;

/**
 * Condition for checking browser properties
 */
public class BrowserCondition implements UserContextCondition, ConditionalAuthenticator {
    private final DeviceContext deviceContext;
    private final List<Operation<DeviceContext>> rules;

    public BrowserCondition(KeycloakSession session, List<Operation<DeviceContext>> rules) {
        this.deviceContext = UserContexts.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    public Optional<String> getBrowserName() {
        return deviceContext.getData().map(DeviceRepresentation::getBrowser).map(browser -> browser.contains("/") ? browser.substring(0, browser.indexOf("/")) : browser);
    }

    public boolean isBrowser(String browser) {
        return getBrowserName().filter(b -> b.startsWith(browser)).isPresent();
    }

    public boolean isDefaultKnownBrowser() {
        return DefaultBrowsers.DEFAULT_BROWSERS.stream().anyMatch(this::isBrowser);
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null) {
            var operation = authConfig.getConfig().get(BrowserConditionFactory.OPERATION_CONFIG);
            var browser = authConfig.getConfig().get(BrowserConditionFactory.BROWSER_CONFIG);

            if (StringUtil.isBlank(operation) || StringUtil.isBlank(browser)) return false;
            return rules.stream()
                    .filter(f -> f.getText().equals(operation))
                    .allMatch(f -> f.match(deviceContext, browser));
        }
        return false;
    }
}
