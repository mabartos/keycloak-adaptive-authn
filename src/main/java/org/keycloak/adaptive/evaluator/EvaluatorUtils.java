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
package org.keycloak.adaptive.evaluator;

import com.apicatalog.jsonld.StringUtils;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class EvaluatorUtils {

    private static Optional<String> getWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorFactory)))
                .filter(StringUtils::isNotBlank);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, double defaultValue) {
        return getWeight(session, evaluatorFactory)
                .map(Double::parseDouble)
                .orElse(defaultValue);
    }

    public static double getStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return getStoredEvaluatorWeight(session, evaluatorFactory, Weight.NORMAL);
    }

    public static boolean existsStoredEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return getWeight(session, evaluatorFactory).isPresent();
    }

    public static void storeEvaluatorWeight(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, double value) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.getWeightConfig(evaluatorFactory), Double.toString(value)));
    }

    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, boolean defaultValue) {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .map(f -> f.getAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluatorFactory)))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public static boolean isEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory) {
        return isEvaluatorEnabled(session, evaluatorFactory, true);
    }

    public static void setEvaluatorEnabled(KeycloakSession session, Class<? extends RiskEvaluatorFactory> evaluatorFactory, boolean enabled) {
        Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRealm)
                .ifPresent(f -> f.setAttribute(RiskEvaluatorFactory.isEnabledConfig(evaluatorFactory), Boolean.valueOf(enabled).toString()));
    }
}
