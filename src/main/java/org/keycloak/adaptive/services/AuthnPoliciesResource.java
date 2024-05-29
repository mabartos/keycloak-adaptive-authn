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
import org.jboss.logging.Logger;
import org.keycloak.adaptive.engine.DefaultRiskEngine;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * REST API for Authentication policies
 * Path: '/authn-policies'
 */
@Provider
public class AuthnPoliciesResource implements RealmResourceProvider {
    private static final Logger logger = Logger.getLogger(AuthnPoliciesResource.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthnPolicyProvider provider;

    public AuthnPoliciesResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(AuthnPolicyProvider.class);
    }

    @Path("/parent")
    @OPTIONS
    public Response parentPreflight() {
        return Cors.builder().auth().preflight().allowAllOrigins().allowedMethods().add(Response.ok());
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
    public List<AuthenticationExecutionInfoRepresentation> getPolicies() {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        AtomicInteger index = new AtomicInteger(0);
        return provider.getAllStream()
                .map(policy -> {
                    var execution = realm.getAuthenticationExecutionByFlowId(policy.getId());
                    var rep = new AuthenticationExecutionInfoRepresentation();

                    rep.setLevel(0);
                    rep.setIndex(index.getAndIncrement());
                    rep.setRequirementChoices(List.of(
                            AuthenticationExecutionModel.Requirement.CONDITIONAL.name(),
                            AuthenticationExecutionModel.Requirement.DISABLED.name())
                    );
                    rep.setDisplayName(policy.getAlias());
                    rep.setDescription(policy.getDescription());
                    rep.setConfigurable(false);
                    rep.setId(execution.getId());
                    rep.setAuthenticationFlow(true);
                    rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                    rep.setRequirement(execution.getRequirement().name());
                    rep.setFlowId(execution.getFlowId());

                    return rep;
                }).toList();
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
        var policy = provider.getByExecutionId(policyId).orElseThrow(() -> new NotFoundException("Could not find policy by id"));
        return new AuthnPolicyResource(session, provider, policy);
    }

    @Path("{any:.*}")
    @OPTIONS
    public Response policyPreflight(@PathParam("any") String any) {
        logger.debug("PREFLIGHT:" + any);
        return Cors.builder().auth().preflight().allowAllOrigins().allowedMethods().add(Response.ok());
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
