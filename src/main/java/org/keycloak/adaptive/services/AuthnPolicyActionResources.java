package org.keycloak.adaptive.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.models.AuthnPolicyActionBasic;
import org.keycloak.adaptive.models.AuthnPolicyActionModel;
import org.keycloak.adaptive.models.AuthnPolicyActionRepresentation;
import org.keycloak.adaptive.models.AuthnPolicyConditionModel;
import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.deployment.DeployedConfigurationsManager;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.utils.CredentialHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.keycloak.adaptive.services.AuthnPoliciesResource.getNextPriority;

public class AuthnPolicyActionResources {
    private static final Logger logger = Logger.getLogger(AuthnPolicyResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthnPolicyModel policy;

    public AuthnPolicyActionResources(KeycloakSession session, AuthnPolicyModel policy) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthnPolicyActionRepresentation> getActions() {
        AtomicInteger index = new AtomicInteger(0);

        return realm.getAuthenticationExecutionsStream(policy.getId()).map(condition -> {
            AuthnPolicyActionRepresentation rep = new AuthnPolicyActionRepresentation();
            rep.setLevel(0);
            rep.setIndex(index.getAndIncrement());
            rep.setRequirementChoices(List.of(AuthenticationExecutionModel.Requirement.REQUIRED.name()));

            String providerId = condition.getAuthenticator();
            ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
            if (factory == null) {
                logger.warnf("Cannot find condition provider implementation with provider ID '%s'", providerId);
                throw new NotFoundException("Could not find condition provider");
            }
            rep.setDisplayName(factory.getDisplayType());
            rep.setConfigurable(factory.isConfigurable());
            rep.setId(policy.getId());
            rep.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL.name());

            if (factory.isConfigurable()) {
                String authenticatorConfigId = condition.getAuthenticatorConfig();
                if (authenticatorConfigId != null) {
                    AuthenticatorConfigModel authenticatorConfig = new DeployedConfigurationsManager(session).getAuthenticatorConfig(realm, authenticatorConfigId);

                    if (authenticatorConfig != null) {
                        rep.setAlias(authenticatorConfig.getAlias());
                    }
                }
            }

            rep.setProviderId(providerId);
            rep.setAuthenticationConfig(condition.getAuthenticatorConfig());

            return rep;
        }).toList();
    }

    @POST
    @Path("/actions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAction(AuthnPolicyActionBasic action) {
        AuthnPolicyActionModel model = (AuthnPolicyActionModel) RepresentationToModel.toModel(session, realm, action);

        var authenticator = session.getProvider(Authenticator.class, model.getAuthenticator());
        if (!(authenticator instanceof ConditionalAuthenticator)) {
            throw new BadRequestException("Condition must implement ConditionalAuthenticator interface");
        }

        model.setParentFlow(policy.getId());

        model.setPriority(getNextPriority(realm, policy));
        model = (AuthnPolicyActionModel) realm.addAuthenticatorExecution(model);
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    @Path("/{actionId}")
    public AuthnPolicyActionResource forwardToPolicyCondition(@PathParam("actionId") String actionId) {
        AuthnPolicyActionModel model = (AuthnPolicyActionModel) realm.getAuthenticationExecutionById(actionId);
        if (model == null) {
            logger.debugf("Cannot find execution with id: %s", actionId);
            throw new NotFoundException("Cannot find action");
        }
        return new AuthnPolicyActionResource(session, policy, model);
    }
}
