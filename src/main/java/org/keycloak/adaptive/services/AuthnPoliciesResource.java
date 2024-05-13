/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.services;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class AuthnPoliciesResource implements RealmResourceProvider {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthnPolicyProvider provider;

    public AuthnPoliciesResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(AuthnPolicyProvider.class);
    }

    @Path("/parent")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getParentPolicy() {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        return Optional.ofNullable(provider.getOrCreateParentPolicy())
                .map(f -> ModelToRepresentation.toRepresentation(session, realm, f))
                .orElseThrow(() -> new NotFoundException("Cannot find parent policy"));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<AuthenticationFlowRepresentation> getPolicies() {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        return provider.getAllStream()
                .map(f -> ModelToRepresentation.toRepresentation(session, realm, f))
                .collect(Collectors.toSet());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addPolicy(AuthenticationFlowRepresentation policy) {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        if (StringUtil.isBlank(policy.getAlias())) {
            throw ErrorResponse.exists("Failed to create policy with empty alias name");
        }

        if (realm.getFlowByAlias(policy.getAlias()) != null) {
            throw ErrorResponse.exists("Policy " + policy.getAlias() + " already exists");
        }

        //adding an empty string to avoid NPE
        if (Objects.isNull(policy.getDescription())) {
            policy.setDescription("");
        }

        ReservedCharValidator.validate(policy.getAlias());

        AuthenticationFlowModel createdModel = provider.addPolicy(RepresentationToModel.toModel(policy));
        policy.setId(createdModel.getId());
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(policy.getId()).build()).build();
    }

    @Path("/{policyId}")
    public AuthnPolicyResource forwardToPolicyResource(@PathParam("policyId") String policyId) {
        var policy = provider.getById(policyId).orElseThrow(() -> new NotFoundException("Could not find policy by id"));
        return new AuthnPolicyResource(session, provider, policy);
    }

    @Path("{any:.*}")
    @OPTIONS
    public Response policyPreflight(@PathParam("any") String any) {
        System.err.println("PREFLIGHT:" + any);
        return Cors.builder().auth().preflight().allowAllOrigins().allowedMethods().add(Response.ok());
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }

    public static int getNextPriority(RealmModel realm, AuthenticationFlowModel parentPolicy) {
        var conditions = realm.getAuthenticationExecutionsStream(parentPolicy.getId()).toList();
        return conditions.isEmpty() ? 0 : conditions.get(conditions.size() - 1).getPriority() + 1;
    }
}
