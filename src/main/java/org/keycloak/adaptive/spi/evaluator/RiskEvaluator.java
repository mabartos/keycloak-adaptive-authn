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

import org.keycloak.adaptive.level.Risk;
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Risk evaluator for calculating risk that the authentication user is fraud
 * All risk scores retrieved from evaluators are aggregated in the {@link org.keycloak.adaptive.spi.engine.RiskEngine}
 * Evaluates risk based on provided {@link org.keycloak.adaptive.spi.context.UserContext} data
 */
public interface RiskEvaluator extends Provider {

    /**
     * Get evaluated risk score with additional information
     * Never must be null - return {@link Risk#invalid()} instead
     *
     * @return risk score in range (0,1> with additional parameters
     */
    Risk getRisk();

    /**
     * Evaluation phases in which the risk score evaluation will be executed
     */
    Set<EvaluationPhase> evaluationPhases();

    /**
     * Get weight of the evaluation claims how much the evaluations should influence the overall risk score
     *
     * @return weight of the evaluation in range (0,1>
     */
    double getWeight();

    /**
     * Execute evaluation of the risk score
     */
    void evaluateRisk();

    /**
     * Flag to determine whether the evaluator should evaluate the risk score
     *
     * @return true if is enabled, otherwise false
     */
    boolean isEnabled();

    /**
     * Evaluation phase representing in what phase/situation the risk should be evaluated
     */
    enum EvaluationPhase {
        /**
         * Executed before starting authentication process
         * <p>Useful for evaluating risk for properties without knowing the user such as browser, IP address, device, etc.</p>
         */
        BEFORE_AUTHN,

        /**
         * Executed after determining user during the authentication process.
         * Usually after providing username + password to avoid any security threats.
         * <p>Useful for evaluating risk of the attempting user such as role, login failures, login events, etc.</p>
         */
        USER_KNOWN,

        /**
         * Executed on demand in runtime when some event occurs and the risk score for the authenticated user should be reevaluated.
         * We consider always having information about the authenticated user.
         * <p>Should be used in conjunction with event listener</p>
         */
        CONTINUOUS
    }
}
