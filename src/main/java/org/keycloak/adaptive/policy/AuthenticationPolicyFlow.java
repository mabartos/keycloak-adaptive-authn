package org.keycloak.adaptive.policy;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.DefaultAuthenticationFlow;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// not usable much - use AuthnPolicyProvider
public class AuthenticationPolicyFlow extends DefaultAuthenticationFlow implements AuthenticationFlow {
    public static final String AUTHN_POLICY_FLOW = "authn-policy-flow";
    private static final Logger logger = Logger.getLogger(AuthenticationPolicyFlow.class);

    private final AuthenticationProcessor processor;
    private final AuthenticationFlowModel flow;
    private final List<AuthenticationExecutionModel> executions;

    private boolean successful = false;

    public AuthenticationPolicyFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        super(processor, flow);
        this.processor = processor;
        this.flow = flow;
        this.executions = processor.getRealm().getAuthenticationExecutionsStream(flow.getId()).toList();
    }

    @Override
    public Response processAction(String actionExecution) {
        if (StringUtil.isBlank(actionExecution)) {
            throw new AuthenticationFlowException("action is not in current execution", AuthenticationFlowError.INTERNAL_ERROR);
        }

        var action = executions.stream().filter(f -> f.getId().equals(actionExecution)).findFirst();
        if (action.isEmpty()) {
            throw new AuthenticationFlowException("Execution not found", AuthenticationFlowError.INTERNAL_ERROR);
        }

        return processAction(action.get());
    }

    public Response processAction(AuthenticationExecutionModel action) {
        var authFactory = processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, action.getAuthenticator());
        if (authFactory == null) {
            throw new RuntimeException("Unable to find factory for AuthenticatorFactory: " + action.getAuthenticator() + " did you forget to declare it in a META-INF/services file?");
        }

        var authenticator = authFactory.create(processor.getSession());

        AuthenticationProcessor.Result result = processor.createAuthenticatorContext(action, authenticator, executions);
        authenticator.action(result);
        authenticator.authenticate(result);

        Response response = processResult(result, true);
        if (response == null) {
            throw new AuthenticationFlowException("Cannot execute action", AuthenticationFlowError.INTERNAL_ERROR);
        }
        return response;
    }

    @Override
    public Response processFlow() {
        Map<AuthenticationExecutionModel, ConditionalAuthenticator> conditions = new HashMap<>();
        Map<AuthenticationExecutionModel, Authenticator> actions = new HashMap<>();

        executions.stream()
                .filter(Objects::nonNull)
                .forEach(f -> {
                    var authFactory = processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                    if (authFactory != null) {
                        var authenticator = authFactory.create(processor.getSession());
                        if (authenticator instanceof ConditionalAuthenticator conditionalAuthenticator) {
                            conditions.put(f, conditionalAuthenticator);
                        } else {
                            actions.put(f, authenticator);
                        }
                    }
                });

        var isOk = conditions.entrySet()
                .stream()
                .allMatch((entry) -> {
                    var context = processor.createAuthenticatorContext(entry.getKey(), entry.getValue(), executions);
                    return entry.getValue().matchCondition(context);
                });

        if (isOk) {
            boolean requiredElementsSuccessful = true;

            for (var action : actions.keySet()) {
                var response = processAction(action);
                if (response != null) return response;
                requiredElementsSuccessful &= processor.isSuccessful(action);
            }

            if (requiredElementsSuccessful) {
                successful = true;
            }
        } else {
            logger.warn("Flow is not OK");
        }

        return null;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }
}
