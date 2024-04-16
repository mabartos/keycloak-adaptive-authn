package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Set;

public class BrowserCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final Set<Operation<DeviceContext>> rules;

    public BrowserCondition(KeycloakSession session, Set<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    public String getBrowser() {
        return deviceContext.getData().getBrowser();
    }

    public boolean isBrowser(String browser) {
        return getBrowser().equals(browser);
    }

    public boolean isDefaultKnownBrowser() {
        return DefaultBrowsers.DEFAULT_BROWSERS.stream().anyMatch(f -> f.equals(getBrowser()));
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

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(deviceContext);
    }
}
