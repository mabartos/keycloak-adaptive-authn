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
package org.keycloak.adaptive.evaluator.role;

import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.user.KcUserRoleContextFactory;
import org.keycloak.adaptive.context.user.UserRoleContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

import java.util.Collection;
import java.util.Set;

/**
 * Risk evaluator for user role properties
 */
public class DefaultUserRoleEvaluator extends AbstractRiskEvaluator {
    private final KeycloakSession session;
    private final UserRoleContext context;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
        this.context = UserContexts.getContext(session, KcUserRoleContextFactory.PROVIDER_ID);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate() {
        boolean isAdmin = context.getData()
                .stream()
                .flatMap(Collection::stream)
                .map(RoleModel::getName)
                .anyMatch(roleName -> roleName.equals(AdminRoles.ADMIN) || roleName.equals(AdminRoles.REALM_ADMIN));

        return isAdmin ? Risk.of(Risk.INTERMEDIATE) : Risk.none();
    }
}
