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

package io.github.mabartos.ai.granite;

import org.keycloak.Config;
import io.github.mabartos.spi.ai.AiEngineFactory;
import io.github.mabartos.spi.ai.AiEngine;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;

public class GraniteAiEngineFactory implements AiEngineFactory {
    public static final String PROVIDER_ID = "granite";

    private static final String URL_PROPERTY = "ai.granite.api.url";
    private static final String KEY_PROPERTY = "ai.granite.api.key";
    private static final String MODEL_PROPERTY = "ai.granite.api.model";

    @Override
    public AiEngine create(KeycloakSession session) {
        return new GraniteAiEngine(session);
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

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public static Optional<String> getApiUrl() {
        return Configuration.getOptionalValue(URL_PROPERTY);
    }

    public static Optional<String> getApiKey() {
        return Configuration.getOptionalValue(KEY_PROPERTY);
    }

    public static Optional<String> getModel() {
        return Configuration.getOptionalValue(MODEL_PROPERTY);
    }
}
