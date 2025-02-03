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

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.role.KcUserRoleContextFactory;
import org.keycloak.adaptive.context.role.UserRoleContext;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

import java.util.Optional;

/**
 * Risk evaluator for user role properties
 */
public class DefaultUserRoleEvaluator implements RiskEvaluator {
    private final KeycloakSession session;
    private final UserRoleContext context;
    private Double risk;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
        this.context = ContextUtils.getContext(session, KcUserRoleContextFactory.PROVIDER_ID);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, DefaultUserRoleEvaluatorFactory.class, Weight.IMPORTANT);
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, DefaultUserRoleEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void evaluate() {
        boolean isAdmin = context.getData()
                .stream()
                .map(RoleModel::getName)
                .anyMatch(roleName -> roleName.equals(AdminRoles.ADMIN) || roleName.equals(AdminRoles.REALM_ADMIN));

        risk = isAdmin ? 0.6 : 0.1;
    }
}
