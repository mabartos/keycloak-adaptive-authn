package org.keycloak.adaptive.services;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.adaptive.models.AuthnPolicyRepresentation;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class AuthnPolicyResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final String policy;

    public AuthnPolicyResource(KeycloakSession session, String policy) {
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

}
