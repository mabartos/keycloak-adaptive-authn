package org.keycloak.adaptive.services;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.adaptive.models.AuthnPolicyRepresentation;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class AuthnPoliciesResource implements RealmResourceProvider {
    private final KeycloakSession session;
    private final RealmModel realm;

    public AuthnPoliciesResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<AuthnPolicyRepresentation> getPolicies() {
        return realm.getAuthenticationFlowsStream()
                .filter(f -> f.getProviderId().startsWith("authn-policy")) // TODO not like this
                .map(f -> ModelToRepresentation.toRepresentation(session, realm, f))
                .map(f -> (AuthnPolicyRepresentation) f)
                .collect(Collectors.toSet());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addPolicy(AuthnPolicyRepresentation policy) {

    }

    @Path("/{policyId}")
    public AuthnPolicyResource forwardToPolicyResource(@PathParam("policyId") String policyId) {
        // TODO if not exists, throw NotFoundException
        return new AuthnPolicyResource(session, policyId);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
