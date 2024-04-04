package org.keycloak.adaptive.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.models.AuthnPolicyConditionModel;
import org.keycloak.adaptive.models.AuthnPolicyConditionRepresentation;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class AuthnPolicyConditionResource {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationFlowModel policy;
    private final AuthnPolicyConditionModel condition;

    public AuthnPolicyConditionResource(KeycloakSession session, AuthenticationFlowModel policy, AuthnPolicyConditionModel condition) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
        this.condition = condition;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AuthnPolicyConditionModel getCondition() {
        return condition;
    }

    // TODO add raisePriority
    // TODO add lowerPriority

    @DELETE
    public Response removeCondition() {
        KeycloakModelUtils.deepDeleteAuthenticationExecutor(session, realm, condition,
                () -> {
                }, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("It is illegal to remove condition from a built in flow");
                }
        );
        return Response.noContent().build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public AuthnPolicyConditionRepresentation updateCondition(AuthnPolicyConditionRepresentation condition) {
        return null;
    }
}
