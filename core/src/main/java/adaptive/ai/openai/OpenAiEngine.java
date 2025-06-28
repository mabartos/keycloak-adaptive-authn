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
package org.keycloak.adaptive.ai.openai;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.ai.AiEngineUtils;
import org.keycloak.adaptive.ai.DefaultAiDataRequest;
import org.keycloak.adaptive.ai.DefaultAiDataResponse;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.ai.AiEngine;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Map;
import java.util.Optional;

/**
 * OpenAI ChatGPT engine
 */
public class OpenAiEngine implements AiEngine {
    private static final Logger logger = Logger.getLogger(OpenAiEngine.class);

    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;

    public OpenAiEngine(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
    }

    @Override
    public <T> Optional<T> getResult(String context, String message, Class<T> clazz, DefaultAiDataRequest.ResponseFormat responseFormat) {
        final var url = OpenAiEngineFactory.getApiUrl();
        final var model = OpenAiEngineFactory.getModel();
        final var key = OpenAiEngineFactory.getApiKey();
        final var organization = OpenAiEngineFactory.getOrganization();
        final var project = OpenAiEngineFactory.getProject();

        if (url.isEmpty() || model.isEmpty() || key.isEmpty() || organization.isEmpty() || project.isEmpty()) {
            logger.errorf("Some of required environment variables are missing. Check the guide how to set this AI engine.");
            return Optional.empty();
        }

        var httpClient = httpClientProvider.getHttpClient();

        var result = AiEngineUtils.aiEngineRequest(
                httpClient,
                url.get(),
                () -> DefaultAiDataRequest.newRequest(model.get(), context, message, responseFormat),
                Map.of("Authorization", String.format("Bearer %s", key.get()),
                        "OpenAI-Organization", organization.get(),
                        "OpenAI-Project", project.get()
                ),
                clazz
        );

        logger.tracef("Response from AI engine: %s\n", result.toString());
        return result;
    }

    @Override
    public Risk getRisk(String context, String message) {
        var response = getResult(context, message, DefaultAiDataResponse.class, DefaultAiDataRequest.newJsonResponseFormat("risk_evaluation", AiEngine.DEFAULT_RISK_SCHEMA));
        if (response.isEmpty()) {
            return Risk.invalid();
        }

        return AiEngineUtils.getRiskFromDefaultResponse(response.get(),
                (eval) -> logger.tracef("OpenAI ChatGPT evaluated risk: %f. Reason: %s", eval.risk(), eval.reason())
        );
    }

    @Override
    public void close() {

    }
}
