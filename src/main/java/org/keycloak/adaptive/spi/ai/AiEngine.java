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
package org.keycloak.adaptive.spi.ai;

import org.keycloak.provider.Provider;

import java.util.Optional;

/**
 * Artificial Intelligence Natural Language Processing engine
 */
public interface AiEngine extends Provider {

    /**
     * Get result from the AI engine
     *
     * @param message your query
     * @param clazz   what response type should be returned
     */
    default <T> Optional<T> getResult(String message, Class<T> clazz) {
        return getResult("", message, clazz);
    }

    /**
     * Get result from the AI engine
     *
     * @param context context message representing the overall general instruction for each request
     * @param message your query
     * @param clazz   what response type should be returned
     */
    <T> Optional<T> getResult(String context, String message, Class<T> clazz);

    /**
     * Get evaluated risk score from the AI engine
     *
     * @param message your query
     */
    default Optional<Double> getRisk(String message) {
        return getRisk(AiRiskEvaluatorMessages.getContextMessage(), message);
    }

    /**
     * Get evaluated risk score from the AI engine
     *
     * @param context context message representing the overall general instruction for each request on how to evaluate risk
     * @param message your query
     */
    Optional<Double> getRisk(String context, String message);
}
