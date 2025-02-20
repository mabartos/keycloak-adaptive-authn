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

import org.keycloak.provider.Provider;

import java.util.Optional;

/**
 * Provider for storing the evaluated overall risk score through the authentication flow processing
 * The overall risk score is calculated in several phases and then aggregated
 */
public interface StoredRiskProvider extends Provider {

    /**
     * Get evaluated overall stored risk score
     *
     * @return overall risk score in range (0,1>
     */
    Optional<Double> getStoredRisk();

    /**
     * Get evaluated stored risk score for the specific phase
     *
     * @param riskPhase phase of the evaluation
     * @return overall risk score in range (0,1>
     */
    Optional<Double> getStoredRisk(RiskPhase riskPhase);

    /**
     * Store the overall risk score
     *
     * @param risk overall risk score in range (0,1>
     */
    void storeRisk(double risk);

    /**
     * Store the overall risk score for the specific phase
     *
     * @param risk      overall risk score in range (0,1>
     * @param riskPhase phase of the evaluation
     */
    void storeRisk(double risk, RiskPhase riskPhase);

    /**
     * Individual risk score phases
     */
    enum RiskPhase {
        NO_USER, // Phase for evaluated risk score based on evaluators that do NOT REQUIRE user
        REQUIRES_USER, // Phase for evaluated risk score based on evaluators that do REQUIRE user
        OVERALL // Phase for the overall risk score aggregating the NO_USER and REQUIRES_USER scores
    }

    /**
     * Get stored overall risk score in a printable version
     */
    default Optional<String> printStoredRisk() {
        return printStoredRisk(RiskPhase.OVERALL);
    }

    /**
     * Get stored risk score in a printable version for specific risk phase
     */
    default Optional<String> printStoredRisk(RiskPhase riskPhase) {
        return getStoredRisk(riskPhase).map(score -> String.format("%.2f", score));
    }
}