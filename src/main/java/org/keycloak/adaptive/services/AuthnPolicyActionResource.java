package org.keycloak.adaptive.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.models.AuthnPolicyActionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class AuthnPolicyActionResource {
    private static final Logger logger = Logger.getLogger(AuthnPolicyConditionResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationFlowModel policy;
    private final AuthnPolicyActionModel action;

    public AuthnPolicyActionResource(KeycloakSession session, AuthenticationFlowModel policy, AuthnPolicyActionModel action) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
        this.action = action;
    }

    @GET
    public AuthnPolicyActionModel getAction() {
        return action;
    }

    @DELETE
    @Path("/actions")
    public Response removeAction() {
        KeycloakModelUtils.deepDeleteAuthenticationExecutor(session, realm, action,
                () -> {
                }, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("It is illegal to remove action from a built in flow");
                }
        );
        return Response.noContent().build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Object updateAction() {
        return null;
    }
}
