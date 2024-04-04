package org.keycloak.adaptive.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.adaptive.models.AuthnPolicyRepresentation;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

public class AuthnPolicyResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthnPolicyModel policy;

    public AuthnPolicyResource(KeycloakSession session, AuthnPolicyModel policy) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AuthnPolicyRepresentation getPolicy() {
        return (AuthnPolicyRepresentation) ModelToRepresentation.toRepresentation(session, realm, policy);
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePolicy(AuthnPolicyRepresentation update) {
        if (StringUtil.isBlank(update.getAlias())) {
            throw ErrorResponse.exists("Failed to update policy with empty alias name");
        }

        ReservedCharValidator.validate(update.getAlias());

        update.setId(policy.getId());
        realm.updateAuthenticationFlow(RepresentationToModel.toModel(update));

        return Response.accepted(update).build();
    }

    @DELETE
    public void removePolicy() {
        KeycloakModelUtils.deepDeleteAuthenticationFlow(session, realm, policy,
                () -> {
                }, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("Cannot delete policy");
                }
        );
    }

    @Path("/conditions")
    public AuthnPolicyConditionResources forwardToPolicyConditionResources() {
        return new AuthnPolicyConditionResources(session, policy);
    }

    @Path("/actions")
    public AuthnPolicyActionResources forwardToPolicyActionResources() {
        return new AuthnPolicyActionResources(session, policy);
    }
}
