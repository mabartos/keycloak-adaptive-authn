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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

public class AuthnPolicyResource {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthnPolicyProvider provider;
    private final AuthenticationFlowModel policy;

    public AuthnPolicyResource(KeycloakSession session, AuthnPolicyProvider provider, AuthenticationFlowModel policy) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = provider;
        this.policy = policy;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getPolicy() {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");
        return ModelToRepresentation.toRepresentation(session, realm, policy);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePolicy(AuthenticationFlowRepresentation update) {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        if (StringUtil.isBlank(update.getAlias())) {
            throw ErrorResponse.exists("Failed to update policy with empty alias name");
        }

        ReservedCharValidator.validate(update.getAlias());

        update.setId(policy.getId());

        provider.update(RepresentationToModel.toModel(update));

        return Response.accepted(update).build();
    }

    @DELETE
    public void removePolicy() {
        // TESTING PURPOSE
        session.getContext().getHttpResponse().setHeader("Access-Control-Allow-Origin","*");

        KeycloakModelUtils.deepDeleteAuthenticationFlow(session, realm, policy,
                () -> {
                }, // allow deleting even with missing references
                () -> {
                    throw new BadRequestException("Cannot delete policy");
                }
        );
    }
}
