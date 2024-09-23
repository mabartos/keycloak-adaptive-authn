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

package org.keycloak.adaptive.ai;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AiEngineUtils {

    /**
     * Abstraction layer up to the HttpClient to obtain data from common AI NLP engines
     */
    public static <T> T aiEngineRequest(
            CloseableHttpClient client,
            String url,
            Supplier<Object> body,
            Map<String, String> requestHeaders,
            Class<T> resultClass
    ) {
        try {
            var request = new HttpPost(new URIBuilder(url).build());
            request.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            request.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

            requestHeaders.forEach(request::setHeader);

            request.setEntity(new StringEntity(JsonSerialization.writeValueAsString(body.get()), ContentType.APPLICATION_JSON));

            try (var response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new RuntimeException(response.getStatusLine().toString());
                }
                var result = JsonSerialization.readValue(response.getEntity().getContent(), resultClass);
                EntityUtils.consumeQuietly(response.getEntity());
                return result;
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Abstraction layer up to the DefaultAiDataResponse to obtain risk data from common AI NLP engines
     */
    public static Optional<Double> getRiskFromDefaultResponse(DefaultAiDataResponse response, Consumer<DefaultAiRiskData> additionalOps) {
        var data = Optional.ofNullable(response)
                .flatMap(f -> f.choices().stream().findAny())
                .map(DefaultAiDataResponse.Choice::message)
                .map(DefaultAiDataResponse.Choice.Message::content)
                .map(f -> {
                    try {
                        return JsonSerialization.readValue(f, DefaultAiRiskData.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        data.ifPresent(additionalOps);
        return data.map(DefaultAiRiskData::risk);
    }
}
