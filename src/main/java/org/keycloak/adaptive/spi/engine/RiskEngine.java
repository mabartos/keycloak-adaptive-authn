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
package org.keycloak.adaptive.spi.engine;

import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

/**
 * Risk engine for aggregating risk scores from the {@link RiskEvaluator}s and calculating the overall risk score for
 * the whole authentication request.
 */
public interface RiskEngine extends Authenticator, ConfigurableRequirements {
    /**
     * Get the overall risk score for the authentication request
     *
     * @return risk score in range (0,1>
     */
    Double getRisk();

    /**
     * Risk evaluators that contributes to the overall risk score calculations
     *
     * @return set of risk evaluators
     */
    Set<RiskEvaluator> getRiskEvaluators();

    /**
     * Start the overall risk score evaluation
     */
    void evaluateRisk();

    @Override
    default void action(AuthenticationFlowContext context) {
    }

    @Override
    default boolean requiresUser(AuthenticatorConfigModel config) {
        return requiresUser();
    }

    @Override
    default boolean requiresUser() {
        return false;
    }

    @Override
    default boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    default void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    default void close() {

    }

    static boolean isValidValue(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return false;
        return value >= 0.0d && value <= 1.0d;
    }
}
