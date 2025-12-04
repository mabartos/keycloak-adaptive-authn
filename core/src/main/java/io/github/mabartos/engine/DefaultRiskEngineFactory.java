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
package io.github.mabartos.engine;

import org.keycloak.Config;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.RiskEngineFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.time.Duration;
public class DefaultRiskEngineFactory implements RiskEngineFactory {
    public static final String PROVIDER_ID = "default";

    public static final Duration DEFAULT_EVALUATOR_TIMEOUT = Duration.ofMillis(2500L);
    public static final int DEFAULT_EVALUATOR_RETRIES = 3;

    public static final String EVALUATOR_TIMEOUT_CONFIG = "riskEvaluatorTimeoutConfig";
    public static final String EVALUATOR_RETRIES_CONFIG = "riskEvaluatorRetriesConfig";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new DefaultRiskEngine(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }
}
