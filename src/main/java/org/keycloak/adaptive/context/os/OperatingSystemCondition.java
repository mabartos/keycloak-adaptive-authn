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
import org.keycloak.utils.StringUtil;

import java.util.Set;

public class OperatingSystemCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final Set<Operation<DeviceContext>> rules;

    public OperatingSystemCondition(KeycloakSession session, Set<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(deviceContext);
    }

    public boolean isOs(String os) {
        return OperatingSystemConditionFactory.isOs(deviceContext, os);
    }

    public boolean isDefaultKnownOs() {
        return DefaultOperatingSystems.DEFAULT_OPERATING_SYSTEMS.stream().anyMatch(this::isOs);
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null) {
            var operation = authConfig.getConfig().get(OperatingSystemConditionFactory.OPERATION_CONFIG);
            var os = authConfig.getConfig().get(OperatingSystemConditionFactory.OS_CONFIG);

            if (StringUtil.isBlank(operation) || StringUtil.isBlank(os)) return false;
            return rules.stream()
                    .filter(f -> f.getText().equals(operation))
                    .allMatch(f -> f.match(deviceContext, os));
        }
        return false;
    }
}
