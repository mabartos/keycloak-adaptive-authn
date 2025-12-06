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
package io.github.mabartos.spi.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Risk engine for aggregating risk scores from the {@link RiskEvaluator}s and calculating the overall risk score for
 * the whole authentication request.
 */
public interface RiskEngine extends Provider {
    /**
     * Default period for continuous risk evaluation in minutes
     */
    int DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES = 10;

    /**
     * Get the overall risk score for the authentication request
     * Never must be null - return {@link Risk#invalid()} instead
     *
     * @return risk score in range (0,1> with additional parameters
     */
    Risk getOverallRisk();

    /**
     * Get the risk score for the specific {@link RiskEvaluator.EvaluationPhase} evaluation phase
     * Never must be null - return {@link Risk#invalid()} instead
     *
     * @return risk score in range (0,1> with additional parameters
     */
    Risk getRisk(RiskEvaluator.EvaluationPhase phase);

    /**
     * Risk evaluators that contributes to the overall risk score calculations based on the requirement of knowing the user
     *
     * @return set of risk evaluators
     */
    Set<RiskEvaluator> getRiskEvaluators(RiskEvaluator.EvaluationPhase evaluationPhase);

    /**
     * Start the risk scores evaluation for specific evaluation phase
     *
     * @param evaluationPhase {@link RiskEvaluator.EvaluationPhase}
     */
    void evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase);

    /**
     * Start the risk scores evaluation for evaluation phase
     *
     * @param evaluationPhase {@link RiskEvaluator.EvaluationPhase}
     * @param knownUser       information about the user - if the {@code #evaluationPhase} is {@link RiskEvaluator.EvaluationPhase#BEFORE_AUTHN}, the {@code #knownUser} is null
     */
    void evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase, RealmModel realm, UserModel knownUser);

    /**
     * Check if risk-based authentication is enabled for the current realm
     *
     * @return true if risk-based authentication is enabled, false otherwise
     */
    boolean isRiskBasedAuthnEnabled();

    @Override
    default void close() {

    }
}
