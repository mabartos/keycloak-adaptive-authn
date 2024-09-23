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

package org.keycloak.adaptive.ai.granite;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.ai.AiEngineUtils;
import org.keycloak.adaptive.ai.DefaultAiDataRequest;
import org.keycloak.adaptive.ai.DefaultAiDataResponse;
import org.keycloak.adaptive.ai.openai.OpenAiEngine;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Map;
import java.util.Optional;

public class GraniteAiEngine implements AiNlpEngine {
    private static final Logger logger = Logger.getLogger(OpenAiEngine.class);

    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;

    public GraniteAiEngine(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
    }

    @Override
    public <T> T getResult(String context, String message, Class<T> clazz) {
        final var url = System.getenv(GraniteAiEngineFactory.URL_PROPERTY);
        final var key = System.getenv(GraniteAiEngineFactory.KEY_PROPERTY);
        final var model = Optional.ofNullable(System.getenv(GraniteAiEngineFactory.MODEL_PROPERTY)).orElse(GraniteAiEngineFactory.DEFAULT_MODEL);

        if (StringUtil.isBlank(url) || StringUtil.isBlank(key) || StringUtil.isBlank(model)) {
            logger.errorf("Some of these required environment variables are missing: %s, %s, %s\n",
                    GraniteAiEngineFactory.URL_PROPERTY, GraniteAiEngineFactory.KEY_PROPERTY, GraniteAiEngineFactory.MODEL_PROPERTY);
            return null;
        }

        var httpClient = httpClientProvider.getHttpClient();

        var result = AiEngineUtils.aiEngineRequest(
                httpClient,
                url,
                () -> DefaultAiDataRequest.newRequest(model, context, message),
                Map.of("Authorization", String.format("Bearer %s", key)),
                clazz
        );

        logger.debugf("Response from AI engine: %s\n", result.toString());

        return result;
    }

    @Override
    public Optional<Double> getRisk(String context, String message) {
        var response = getResult(context, message, DefaultAiDataResponse.class);

        return AiEngineUtils.getRiskFromDefaultResponse(response,
                (eval) -> logger.debugf("Granite evaluated risk: %f. Reason: %s", eval.risk(), eval.reason())
        );
    }

    @Override
    public void close() {

    }
}
