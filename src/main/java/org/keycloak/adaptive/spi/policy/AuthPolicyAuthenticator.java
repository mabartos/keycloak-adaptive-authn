package org.keycloak.adaptive.spi.policy;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.services.AuthnPolicyConditionResource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthPolicyAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();
        var authPolicies = realm.getAuthenticationFlowsStream()
                .filter(f -> f.getDescription().startsWith("POLICY -")) // TODO have better approach how to determine it's auth policy flow
                .toList();

        boolean isSuccessful = true;

        for (var policy : authPolicies) {
            Map<AuthenticationExecutionModel, ConditionalAuthenticator> conditions = new HashMap<>();
            Map<AuthenticationExecutionModel, Authenticator> actions = new HashMap<>();

            realm.getAuthenticationExecutionsStream(policy.getId())
                    .filter(Objects::nonNull)
                    .forEach(f -> {
                        var authFactory = context.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, f.getAuthenticator());
                        if (authFactory != null) {
                            var authenticator = authFactory.create(session);
                            if (authenticator instanceof ConditionalAuthenticator conditionalAuthenticator) {
                                conditions.put(f, conditionalAuthenticator);
                            } else {
                                actions.put(f, authenticator);
                            }
                        }
                    });

            var allConditionsMatch = conditions.entrySet()
                    .stream()
                    .allMatch((entry) -> {
                        var finalContext = new AuthenticationFlowContextWrapper(realm, context, entry.getKey().getAuthenticatorConfig()); // TODO not very good
                        return entry.getValue().matchCondition(finalContext);
                    });

            if (allConditionsMatch) {
                actions.values().forEach(f -> f.authenticate(context));
                isSuccessful &= actions.keySet().stream().allMatch(f -> isSuccessful(context.getAuthenticationSession(), f));
            } else {
                context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);
                logger.warn("Flow is not OK");
                break;
            }
        }

        if (isSuccessful) {
            context.success();
        } else {
            logger.debug("Auth policies evaluated to false");
            context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);
        }
    }

    public static boolean isSuccessful(AuthenticationSessionModel session, AuthenticationExecutionModel model) {
        AuthenticationSessionModel.ExecutionStatus status = session.getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.SUCCESS;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
