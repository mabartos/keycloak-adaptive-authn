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
package org.keycloak.authn.policy;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authn.policy.spi.AuthnPolicyProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;

import java.util.Optional;

/**
 * Custom authenticator for evaluating authn policies - handle whole flows
 */
public class AuthnPolicyAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        final var provider = session.getProvider(AuthnPolicyProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Cannot find AuthnPolicyProvider");
        }

        var requiresUser = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(f -> f.get(AuthnPolicyAuthenticatorFactory.REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .orElse(null);

        if (requiresUser == null) return;

        final AuthenticationProcessor processor = createProcessor(session, realm, context);

        var policies = provider.getAllStream(requiresUser).toList();

        for (var policy : policies) {
            processor.setFlowId(policy.getId());

            AuthenticationFlow flow = processor.createFlowExecution(policy.getId(), realm.getAuthenticationExecutionByFlowId(policy.getId()));
            Response response = flow.processFlow();

            if (flow.isSuccessful()) {
                continue;
            }

            if (response != null) {
                if (response.getStatus() >= 400) {
                    final AuthenticationFlowError error = Optional.ofNullable(context.getEvent())
                            .map(EventBuilder::getEvent)
                            .map(Event::getError)
                            .map(String::toUpperCase)
                            .map(AuthenticationFlowError::valueOf)
                            .orElse(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);

                    context.failure(error, response);
                } else {
                    // TODO handle challenged sub-executions
                    //context.getAuthenticationSession().setAuthNote(CURRENT_AUTHENTICATION_EXECUTION, xxxx);
                    context.challenge(response);
                }
                return;
            }
        }

        if (context.getStatus() == null) {
            var execution = realm.getAuthenticationExecutionById(context.getExecution().getId());
            if (execution.isAlternative()) {
                context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR);
            } else {
                context.success();
            }
        }
    }

    protected AuthenticationProcessor createProcessor(KeycloakSession session, RealmModel realm, AuthenticationFlowContext context) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setRealm(realm)
                .setAuthenticationSession(session.getContext().getAuthenticationSession())
                .setFlowId(realm.getBrowserFlow().getId())
                .setConnection(session.getContext().getConnection())
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(session.getContext().getHttpRequest())
                .setBrowserFlow(true)
                .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                .setEventBuilder(context.getEvent());
        return processor;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
