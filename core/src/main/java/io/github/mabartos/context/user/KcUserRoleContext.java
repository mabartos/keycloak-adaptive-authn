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
package io.github.mabartos.context.user;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Obtain user role from the authentication session
 */
public class KcUserRoleContext extends UserRoleContext {
    private final KeycloakSession session;

    public KcUserRoleContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<Set<RoleModel>> initData() {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getAuthenticationSession)
                .map(AuthenticationSessionModel::getAuthenticatedUser)
                .map(RoleMapperModel::getRoleMappingsStream)
                .map(f -> f.collect(Collectors.toSet()));
    }
}
