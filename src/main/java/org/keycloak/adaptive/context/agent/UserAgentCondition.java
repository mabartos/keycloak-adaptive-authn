package org.keycloak.adaptive.context.agent;

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

public class UserAgentCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final DeviceContext userContext;
    private final Set<Operation<DeviceContext>> rules;

    public UserAgentCondition(KeycloakSession session, DeviceContext userContext, Set<Operation<DeviceContext>> rules) {
        this.session = session;
        this.userContext = userContext;
        this.rules = rules;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null) {
            var operation = authConfig.getConfig().get(UserAgentConditionFactory.OPERATION_CONFIG);
            var userAgent = authConfig.getConfig().get(UserAgentConditionFactory.USER_AGENT_CONFIG);

            if (StringUtil.isBlank(operation) || StringUtil.isBlank(userAgent)) return false;
            return rules.stream()
                    .filter(f -> f.getText().equals(operation))
                    .allMatch(f -> f.match(userContext, userAgent));
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
