package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.spi.policy.Operation;
import org.keycloak.adaptive.spi.policy.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.Set;

public class OperatingSystemCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final Set<Operation<DeviceContext>> rules;

    public OperatingSystemCondition(KeycloakSession session, DeviceContext deviceContext, Set<Operation<DeviceContext>> rules) {
        this.session = session;
        this.deviceContext = deviceContext;
        this.rules = rules;
    }

    @Override
    public void close() {
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

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }
}
