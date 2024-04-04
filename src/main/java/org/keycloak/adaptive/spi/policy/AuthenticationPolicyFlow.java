package org.keycloak.adaptive.spi.policy;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationFlowModel;

import java.util.Objects;

public class AuthenticationPolicyFlow implements AuthenticationFlow {
    public static final String AUTHN_POLICY_FLOW = "authn-policy-flow";

    private final AuthenticationProcessor processor;
    private final AuthenticationFlowModel flow;

    public AuthenticationPolicyFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        this.processor = processor;
        this.flow = flow;
    }

    @Override
    public Response processAction(String actionExecution) {
        return null;
    }

    @Override
    public Response processFlow() {
        var list = processor.getRealm().getAuthenticationExecutionsStream(flow.getId()).toList();

        var allMatch = list.stream()
                .filter(Objects::nonNull)
                .allMatch(f -> {
                    var authFactory = processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                    if (authFactory == null) return false;
                    var authenticator = authFactory.create(processor.getSession());
                    if (!(authenticator instanceof ConditionalAuthenticator)) return false;

                    var context = processor.createAuthenticatorContext(f, authenticator, list);
                    return ((ConditionalAuthenticator) authenticator).matchCondition(context);
                });

       /* var actions = new ArrayList<>(list).removeAll(conditions);

        for (var condition : conditions) {
            processor.createAuthenticatorContext(condition, )
        }*/

        return null;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }
}
