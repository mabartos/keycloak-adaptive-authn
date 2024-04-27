package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.condition.Operation;
import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Set;

public class PhoneCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final List<Operation<DeviceContext>> rules;

    public PhoneCondition(KeycloakSession session, List<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(deviceContext);
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
