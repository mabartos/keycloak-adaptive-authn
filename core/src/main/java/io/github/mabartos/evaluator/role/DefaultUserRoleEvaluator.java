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
import io.github.mabartos.context.user.UserRoleContext;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Collection;
import java.util.Set;

/**
 * Risk evaluator for user realm role properties.
 * <p>
 * Uses Keycloak's role naming convention to classify risk by prefix.
 * <p>
 * Users with no sensitive realm roles receive a trust signal ({@link Risk.Score#NEGATIVE_LOW}).
 */
@EvaluationPhase(USER_KNOWN)
public class DefaultUserRoleEvaluator extends AbstractRiskEvaluator {
    private static final String MANAGE_PREFIX = "manage-";
    private static final String CREATE_PREFIX = "create-";
    private static final String VIEW_PREFIX = "view-";
    private static final String QUERY_PREFIX = "query-";

    private static final Set<String> SENSITIVE_ROLES = Set.of(
            AdminRoles.ADMIN,
            AdminRoles.REALM_ADMIN,
            AdminRoles.CREATE_REALM,
            AdminRoles.IMPERSONATION
    );

    private final UserRoleContext context;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.context = UserContexts.getContext(session, UserRoleContext.class);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        Risk highest = Risk.of(Risk.Score.NEGATIVE_LOW, "User has no sensitive realm roles");

        var roles = context.getData(realm, knownUser)
                .stream()
                .flatMap(Collection::stream)
                .toList();

        for (RoleModel role : roles) {
            Risk.Score score = scoreForRole(role.getName());
            highest = highest.max(Risk.of(score, "Realm role '%s'".formatted(role.getName())));
        }

        return highest;
    }

    private Risk.Score scoreForRole(String roleName) {
        if (SENSITIVE_ROLES.contains(roleName)) return Risk.Score.MEDIUM;
        if (roleName.startsWith(MANAGE_PREFIX)) return Risk.Score.MEDIUM;
        if (roleName.startsWith(CREATE_PREFIX)) return Risk.Score.SMALL;
        if (roleName.startsWith(VIEW_PREFIX)) return Risk.Score.NONE;
        if (roleName.startsWith(QUERY_PREFIX)) return Risk.Score.NONE;
        return Risk.Score.NONE;
    }
}
