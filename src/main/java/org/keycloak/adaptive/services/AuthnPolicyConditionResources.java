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
import org.keycloak.adaptive.models.AuthnPolicyConditionBasic;
import org.keycloak.adaptive.models.AuthnPolicyConditionModel;
import org.keycloak.adaptive.models.AuthnPolicyConditionRepresentation;
import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
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

public class AuthnPolicyConditionResources {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResources.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationFlowModel policy;

    public AuthnPolicyConditionResources(KeycloakSession session, AuthenticationFlowModel policy) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO should accept only ConditionAuthenticators
    public List<AuthnPolicyConditionRepresentation> getConditions() {
        AtomicInteger index = new AtomicInteger(0);

        return realm.getAuthenticationExecutionsStream(policy.getId()).map(condition -> {
            AuthnPolicyConditionRepresentation rep = new AuthnPolicyConditionRepresentation();
            rep.setLevel(0);
            rep.setIndex(index.getAndIncrement());
            rep.setRequirementChoices(List.of(AuthenticationExecutionModel.Requirement.CONDITIONAL.name()));

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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCondition(AuthnPolicyConditionBasic condition) {
        AuthnPolicyConditionModel model = (AuthnPolicyConditionModel) RepresentationToModel.toModel(session, realm, condition);

        AuthnPolicyModel parentPolicy = (AuthnPolicyModel) realm.getAuthenticationFlowById(model.getParentFlow());
        if (parentPolicy == null) {
            throw new BadRequestException("condition parent policy does not exist");
        }

        model.setPriority(getNextPriority(parentPolicy));
        model = (AuthnPolicyConditionModel) realm.addAuthenticatorExecution(model);
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    @Path("/{conditionId}")
    public AuthnPolicyConditionResource forwardToPolicyCondition(@PathParam("conditionId") String conditionId) {
        AuthnPolicyConditionModel model = (AuthnPolicyConditionModel) realm.getAuthenticationExecutionById(conditionId);
        if (model == null) {
            logger.debugf("Cannot find execution with id: %s", conditionId);
            throw new NotFoundException("Cannot find condition");
        }
        return new AuthnPolicyConditionResource(session, policy, model);
    }

    protected int getNextPriority(AuthnPolicyModel parentPolicy) {
        var conditions = realm.getAuthenticationExecutionsStream(parentPolicy.getId()).toList();
        return conditions.isEmpty() ? 0 : conditions.get(conditions.size() - 1).getPriority() + 1;
    }


}
