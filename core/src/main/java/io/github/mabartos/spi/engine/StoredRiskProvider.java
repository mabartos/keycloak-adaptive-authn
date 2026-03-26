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

import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Optional;

/**
 * Provider for storing phase-level data and the overall risk score through the authentication flow.
 * This is a pure storage layer with no business logic - algorithms decide what to store and how to combine.
 */
public interface StoredRiskProvider extends Provider {

    /**
     * Store multiple attributes for a specific phase.
     * Each key can have multiple values.
     *
     * @param phase      phase of the evaluation
     * @param attributes multi-valued attributes to store
     */
    void storePhaseAttributes(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull MultivaluedHashMap<String, String> attributes);

    /**
     * Get all stored attributes for a specific phase.
     *
     * @param phase phase of the evaluation
     * @return multi-valued attributes, or empty map if none stored
     */
    @Nonnull
    MultivaluedHashMap<String, String> getPhaseAttributes(@Nonnull RiskEvaluator.EvaluationPhase phase);

    /**
     * Store a single attribute for a specific phase.
     *
     * @param phase phase of the evaluation
     * @param key   attribute key
     * @param value attribute value
     */
    void storePhaseAttribute(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key, @Nonnull String value);

    /**
     * Get a single attribute value for a specific phase.
     * If the key has multiple values, returns the first one.
     *
     * @param phase phase of the evaluation
     * @param key   attribute key
     * @return attribute value, or null if not found
     */
    @Nullable
    String getPhaseAttribute(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key);

    /**
     * Get all values for a specific attribute key in a phase.
     *
     * @param phase phase of the evaluation
     * @param key   attribute key
     * @return list of values, or empty list if not found
     */
    @Nonnull
    List<String> getPhaseAttributeValues(@Nonnull RiskEvaluator.EvaluationPhase phase, @Nonnull String key);

    /**
     * Store the overall combined risk score.
     *
     * @param risk overall risk score in range (0,1] and other attributes
     */
    void storeOverallRisk(@Nonnull ResultRisk risk);

    /**
     * Get the stored overall risk score.
     *
     * @return overall risk score in range (0,1]
     */
    @Nonnull
    ResultRisk getStoredOverallRisk();

    /**
     * Get stored overall risk score in a printable version.
     */
    default Optional<String> printStoredRisk() {
        return Optional.of(getStoredOverallRisk()).filter(ResultRisk::isValid).map(risk -> String.format("%.2f", risk.getScore()));
    }
}
