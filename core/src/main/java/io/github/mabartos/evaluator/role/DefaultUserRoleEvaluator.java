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
package io.github.mabartos.evaluator.role;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.user.KcUserRoleContextFactory;
import io.github.mabartos.context.user.UserRoleContext;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Collection;
import java.util.Set;

import static io.github.mabartos.level.Risk.Score.MEDIUM;
import static io.github.mabartos.level.Risk.Score.NONE;

/**
 * Risk evaluator for user role properties
 */
public class DefaultUserRoleEvaluator extends AbstractRiskEvaluator {
    private final UserRoleContext context;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.context = UserContexts.getContext(session, UserRoleContext.class);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        boolean isAdmin = context.getData(realm, knownUser)
                .stream()
                .flatMap(Collection::stream)
                .map(RoleModel::getName)
                .anyMatch(roleName -> roleName.equals(AdminRoles.ADMIN) || roleName.equals(AdminRoles.REALM_ADMIN));

        return isAdmin ? Risk.of(MEDIUM) : Risk.of(NONE);
    }
}
