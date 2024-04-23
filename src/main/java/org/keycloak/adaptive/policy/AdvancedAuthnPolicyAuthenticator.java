package org.keycloak.adaptive.policy;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.services.AuthnPolicyConditionResource;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;

import java.util.Optional;
import java.util.stream.Collectors;

// Custom authenticator for evaluating authn policies - handle whole flows
public class AdvancedAuthnPolicyAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        final var provider = session.getProvider(AuthnPolicyProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Cannot find AuthnPolicyProvider");
        }

        var requiresUser = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(AuthnPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(null);

        if (requiresUser == null) return;

        var authPolicies = provider.getAllStream(requiresUser).collect(Collectors.toSet());

        final AuthenticationProcessor processor = createProcessor(session, realm, context);

        for (var policy : authPolicies) {
            processor.setFlowId(policy.getId());

            AuthenticationFlow flow = processor.createFlowExecution(policy.getId(), realm.getAuthenticationExecutionByFlowId(policy.getId()));
            Response response = flow.processFlow();

            if (flow.isSuccessful()) {
                context.success();
                break;
            }

            if (response != null) {
                if (response.getStatus() >= 400) {
                    final AuthenticationFlowError error = Optional.ofNullable(context.getEvent())
                            .map(EventBuilder::getEvent)
                            .map(Event::getError)
                            .map(String::toUpperCase)
                            .map(AuthenticationFlowError::valueOf)
                            .orElse(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);

                    context.failure(error, response);
                } else {
                    context.challenge(response);
                }
                break;
            }
        }

        if (context.getStatus() == null) {
            context.success();
        }
    }

    protected AuthenticationProcessor createProcessor(KeycloakSession session, RealmModel realm, AuthenticationFlowContext context) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setRealm(realm)
                .setAuthenticationSession(session.getContext().getAuthenticationSession())
                .setFlowId(realm.getBrowserFlow().getId())
                .setConnection(session.getContext().getConnection())
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(session.getContext().getHttpRequest())
                .setBrowserFlow(true)
                .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                .setEventBuilder(context.getEvent());
        return processor;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        System.err.println("HERE");
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
