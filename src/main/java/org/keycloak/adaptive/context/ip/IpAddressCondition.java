package org.keycloak.adaptive.context.ip;

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
import org.keycloak.utils.StringUtil;

import java.util.List;
import java.util.Set;

public class IpAddressCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final List<Operation<DeviceContext>> rules;

    public IpAddressCondition(KeycloakSession session, List<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(deviceContext);
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
