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

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Optional;

/**
 * Condition for checking browser properties
 */
public class BrowserCondition implements UserContextCondition, ConditionalAuthenticator {
    private final DeviceRepresentationContext deviceContext;
    private final List<Operation<DeviceRepresentationContext>> rules;

    public BrowserCondition(KeycloakSession session, List<Operation<DeviceRepresentationContext>> rules) {
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContext.class);
        this.rules = rules;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    public Optional<String> getBrowserName(RealmModel realm) {
        return deviceContext.getData(realm).map(DeviceRepresentation::getBrowser).map(browser -> browser.contains("/") ? browser.substring(0, browser.indexOf("/")) : browser);
    }

    public boolean isBrowser(RealmModel realm, String browser) {
        return getBrowserName(realm).filter(b -> b.startsWith(browser)).isPresent();
    }

    public boolean isDefaultKnownBrowser(RealmModel realm) {
        return DefaultBrowsers.DEFAULT_BROWSERS.stream().anyMatch(browser -> isBrowser(realm, browser));
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
                    .allMatch(f -> f.match(context.getRealm(), deviceContext, browser));
        }
        return false;
    }
}
