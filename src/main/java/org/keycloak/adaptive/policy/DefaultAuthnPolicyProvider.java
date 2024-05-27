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
package org.keycloak.adaptive.policy;

import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.utils.StringUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.keycloak.authentication.AuthenticationFlow.BASIC_FLOW;

/**
 * Default implementation of the Authentication policies DAO
 */
public class DefaultAuthnPolicyProvider implements AuthnPolicyProvider {
    public static final String REQUIRES_USER_CONFIG = "requires.user";

    private final KeycloakSession session;
    private final RealmModel realm;
    private AuthenticationFlowModel parent;

    public DefaultAuthnPolicyProvider(KeycloakSession session) {
        this(session, session.getContext().getRealm());
    }

    public DefaultAuthnPolicyProvider(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
    }

    @Override
    public Optional<AuthenticationFlowModel> getParentPolicy() {
        return Optional.ofNullable(realm.getFlowByAlias(DefaultAuthnPolicyFactory.DEFAULT_AUTHN_POLICIES_FLOW_ALIAS));
    }

    @Override
    public AuthenticationFlowModel getOrCreateParentPolicy() {
        if (parent == null) {
            final var foundParent = getParentPolicy();
            if (foundParent.isEmpty()) {
                synchronized (this) {
                    final var foundParentSync = getParentPolicy();
                    if (foundParentSync.isEmpty()) {
                        var parent = new AuthenticationFlowModel();
                        parent.setAlias(DefaultAuthnPolicyFactory.DEFAULT_AUTHN_POLICIES_FLOW_ALIAS);
                        parent.setDescription("Parent Authentication Policy");
                        parent.setProviderId(BASIC_FLOW);
                        parent.setTopLevel(true);
                        parent.setBuiltIn(false);
                        this.parent = realm.addAuthenticationFlow(parent);
                    } else {
                        this.parent = foundParentSync.get();
                    }
                }
            } else {
                this.parent = foundParent.get();
            }
        }
        return parent;
    }

    @Override
    public AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy) {
        if (StringUtil.isBlank(policy.getAlias()))
            throw new IllegalArgumentException("Cannot create an authentication policy with an empty alias");

        if (!policy.getAlias().startsWith(POLICY_PREFIX)) {
            policy.setAlias(POLICY_PREFIX + policy.getAlias());
        }

        final var existing = getByAlias(policy.getAlias());
        if (existing.isPresent()) {
            throw ErrorResponse.exists("New authentication policy alias already exists");
        }

        var parentFlow = getOrCreateParentPolicy();

        var flow = new AuthenticationFlowModel();
        flow.setAlias(policy.getAlias());
        flow.setDescription(policy.getDescription());
        flow.setProviderId(policy.getProviderId());
        flow = realm.addAuthenticationFlow(flow);

        var config = new AuthenticatorConfigModel();
        config.setAlias("authn-policy-config-" + flow.getId());
        config.getConfig().put(REQUIRES_USER_CONFIG, "false");
        config = realm.addAuthenticatorConfig(config);

        var execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        execution.setPriority(getNextPriority(realm, parentFlow));
        execution.setFlowId(flow.getId());
        execution.setAuthenticatorFlow(true);
        execution.setAuthenticatorConfig(config.getId());

        realm.addAuthenticatorExecution(execution);

        return flow;
    }

    @Override
    public Stream<AuthenticationFlowModel> getAllStream() {
        return realm.getAuthenticationExecutionsStream(getOrCreateParentPolicy().getId())
                .filter(AuthenticationExecutionModel::isAuthenticatorFlow)
                .sorted(Comparator.comparingInt(AuthenticationExecutionModel::getPriority))
                .map(f -> realm.getAuthenticationFlowById(f.getFlowId()))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<AuthenticationFlowModel> getAllStream(boolean requiresUser) {
        return getAllStream().filter(f -> Optional.ofNullable(realm.getAuthenticationExecutionByFlowId(f.getId()))
                .map(g -> realm.getAuthenticatorConfigById(g.getAuthenticatorConfig()))
                .map(AuthenticatorConfigModel::getConfig)
                .map(g -> g.get(REQUIRES_USER_CONFIG))
                .map(Boolean::parseBoolean)
                .map(g -> g == requiresUser)
                .orElse(false));
    }

    @Override
    public Optional<AuthenticationFlowModel> getById(String id) {
        return getAllStream().filter(f -> f.getId().equals(id)).findAny();
    }

    @Override
    public Optional<AuthenticationFlowModel> getByExecutionId(String id) {
        return getAllStream().filter(f -> Optional.ofNullable(realm.getAuthenticationExecutionByFlowId(f.getId()))
                        .filter(g -> g.getId().equals(id))
                        .isPresent())
                .findAny();
    }

    @Override
    public Optional<AuthenticationFlowModel> getByAlias(String alias) {
        return getAllStream().filter(f -> f.getAlias().equals(alias)).findAny();
    }

    @Override
    public boolean remove(AuthenticationFlowModel policy) {
        var found = getById(policy.getId());
        if (found.isEmpty()) {
            return false;
        }
        realm.removeAuthenticationFlow(found.get());
        return true;
    }

    @Override
    public void removeAll() {
        getAllStream().forEach(this::remove);
    }

    @Override
    public void update(AuthenticationFlowModel policy) {
        var found = getById(policy.getId());
        if (found.isEmpty()) return;
        realm.updateAuthenticationFlow(policy);
    }

    public static int getNextPriority(RealmModel realm, AuthenticationFlowModel parentPolicy) {
        var conditions = realm.getAuthenticationExecutionsStream(parentPolicy.getId()).toList();
        return conditions.isEmpty() ? 0 : conditions.get(conditions.size() - 1).getPriority() + 1;
    }

    @Override
    public void close() {

    }
}
