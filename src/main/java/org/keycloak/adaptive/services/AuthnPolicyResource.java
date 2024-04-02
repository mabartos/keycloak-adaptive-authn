package org.keycloak.adaptive.services;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.adaptive.models.AuthnPolicyConditionRepresentation;
import org.keycloak.adaptive.models.AuthnPolicyRepresentation;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.List;

public class AuthnPolicyResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationFlowModel policy;

    public AuthnPolicyResource(KeycloakSession session, AuthenticationFlowModel policy) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.policy = policy;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getPolicy() {
        return "POLICY " + policy;
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePolicy(AuthnPolicyRepresentation policy) {
        return Response.ok().build();
    }

    @DELETE
    public void removePolicy() {

    }

    @GET
    @Path("/conditions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthnPolicyConditionRepresentation> getConditions() {
        return null;
        /* return ModelToRepresentation.toRepresentation(session, realm, policy)
                .getAuthenticationExecutions()
                .stream()
                .map(f->f.getAuthenticatorConfig());*/
    }

    @POST
    @Path("/conditions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCondition(AuthnPolicyConditionRepresentation condition) {
        return Response.created(null).build();// TODO
    }

    @DELETE
    @Path("/conditions")
    public Response removeCondition() {
        return Response.noContent().build();
    }

    @PATCH
    @Path("/conditions")
    @Consumes(MediaType.APPLICATION_JSON)
    public AuthnPolicyConditionRepresentation updateCondition(AuthnPolicyConditionRepresentation condition) {
        return null;
    }

    @GET
    @Path("/actions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Object> getActions() {
        return null;
    }

    @POST
    @Path("/actions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAction() {
        return Response.created(null).build();
    }

    @DELETE
    @Path("/conditions")
    public Response removeAction() {
        return Response.noContent().build();
    }

    @PATCH
    @Path("/conditions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object updateAction() {
        return null;
    }

}
