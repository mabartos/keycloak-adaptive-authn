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
package org.keycloak.authn.policy.spi;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.provider.Provider;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * DAO for authentication policies
 */
public interface AuthnPolicyProvider extends Provider {
    String POLICY_PREFIX = "POLICY - ";

    Optional<AuthenticationFlowModel> getParentPolicy();

    AuthenticationFlowModel getOrCreateParentPolicy();

    AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy);

    Stream<AuthenticationFlowModel> getAllStream();

    Stream<AuthenticationFlowModel> getAllStream(boolean requiresUser);

    Optional<AuthenticationFlowModel> getById(String id);

    Optional<AuthenticationFlowModel> getByExecutionId(String id);

    Optional<AuthenticationFlowModel> getByAlias(String alias);

    boolean remove(AuthenticationFlowModel policy);

    void removeAll();

    void update(AuthenticationFlowModel policy);
}
