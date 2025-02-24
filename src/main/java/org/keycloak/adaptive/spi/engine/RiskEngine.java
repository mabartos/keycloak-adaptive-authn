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
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Risk engine for aggregating risk scores from the {@link RiskEvaluator}s and calculating the overall risk score for
 * the whole authentication request.
 */
public interface RiskEngine extends Provider {
    /**
     * Get the overall risk score for the authentication request
     *
     * @return risk score in range (0,1>
     */
    Double getRisk();

    /**
     * Risk evaluators that contributes to the overall risk score calculations based on the requirement of knowing the user
     *
     * @return set of risk evaluators
     */
    Set<RiskEvaluator> getRiskEvaluators(boolean requiresUser);

    /**
     * Start the overall risk score evaluation
     */
    void evaluateRisk(boolean requiresUser);

    @Override
    default void close() {

    }
}
