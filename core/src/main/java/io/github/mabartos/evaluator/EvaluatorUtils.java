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
package io.github.mabartos.evaluator;

import com.apicatalog.jsonld.StringUtils;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class EvaluatorUtils {

    /**
     * Get weight of the required risk evaluator
     */
    private static Optional<String> getWeight(KeycloakSession session, Class<? extends RiskEvaluator> evaluator) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.getWeightConfig(evaluator)))
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Get stored weight of the required risk evaluator
     */
    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluator> evaluator, double defaultValue) {
        return getWeight(session, evaluator)
                .map(Double::parseDouble)
                .orElse(defaultValue);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluator> evaluator) {
        return getStoredEvaluatorWeight(session, evaluator, Weight.NORMAL);
    }

    public static boolean existsStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluator> evaluator) {
        return getWeight(session, evaluator).isPresent();
    }

    /**
     * Store risk evaluator weight in realm attributes
     */
    public static void storeEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluator> evaluator, double value) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.getWeightConfig(evaluator), Double.toString(value)));
    }

    /**
     * Check whether the risk evaluator is enabled on the realm level
     */
    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluator> evaluator, boolean defaultValue) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluator)))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluator> evaluator) {
        return isEvaluatorEnabled(session, evaluator, true);
    }

    public static void setEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluator> evaluator, boolean enabled) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluator), Boolean.valueOf(enabled).toString()));
    }
}
