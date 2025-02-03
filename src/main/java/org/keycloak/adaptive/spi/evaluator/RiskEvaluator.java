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
package org.keycloak.adaptive.spi.evaluator;

import org.keycloak.provider.Provider;

import java.util.Optional;

/**
 * Risk evaluator for calculating risk that the authentication user is fraud
 * All risk scores retrieved from evaluators are aggregated in the {@link org.keycloak.adaptive.spi.engine.RiskEngine}
 * Evaluates risk based on provided {@link org.keycloak.adaptive.spi.context.UserContext} data
 */
public interface RiskEvaluator extends Provider {

    /**
     * Get evaluated risk score
     *
     * @return (optional) risk score in range (0,1>
     */
    Optional<Double> getRiskValue();

    /**
     * Get weight of the evaluation claims how much the evaluations should influence the overall risk score
     *
     * @return weight of the evaluation in range (0,1>
     */
    double getWeight();

    /**
     * Flag to determine whether the evaluator requires user information to properly calculate the risk score
     *
     * @return true if the user info is required, otherwise false
     */
    boolean requiresUser();

    /**
     * Execute evaluation of the risk score
     */
    void evaluate();

    /**
     * Flag to determine whether the evaluator should evaluate the risk score
     *
     * @return true if is enabled, otherwise false
     */
    default boolean isEnabled() {
        return true;
    }

    default void close() {
    }
}
