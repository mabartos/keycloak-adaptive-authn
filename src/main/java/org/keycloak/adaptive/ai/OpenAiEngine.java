package org.keycloak.adaptive.ai;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.keycloak.adaptive.ai.OpenAiEngineFactory.KEY_PROPERTY;
import static org.keycloak.adaptive.ai.OpenAiEngineFactory.ORGANIZATION_PROPERTY;
import static org.keycloak.adaptive.ai.OpenAiEngineFactory.PROJECT_PROPERTY;
import static org.keycloak.adaptive.ai.OpenAiEngineFactory.URL_PROPERTY;

public class OpenAiEngine implements AiNlpEngine {
    private static final Logger logger = Logger.getLogger(OpenAiEngine.class);

    private final KeycloakSession session;
    private final HttpClientProvider httpClientProvider;

    public OpenAiEngine(KeycloakSession session) {
        this.session = session;
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
    }

    @Override
    public <T> T getResult(String context, String message, Class<T> clazz) {
        try {
            final var url = Configuration.getOptionalValue(URL_PROPERTY);
            final var key = Configuration.getOptionalValue(KEY_PROPERTY);
            final var organization = Configuration.getOptionalValue(ORGANIZATION_PROPERTY);
            final var project = Configuration.getOptionalValue(PROJECT_PROPERTY);

            if (url.isEmpty() || key.isEmpty() || organization.isEmpty() || project.isEmpty()) {
                logger.error("Some of these required environment variables is missing: OPEN_AI_API_URL, OPEN_AI_API_KEY, OPEN_AI_API_ORGANIZATION, OPEN_AI_API_PROJECT");
                return null;
            }

            var client = httpClientProvider.getHttpClient();

            var request = new HttpPost(new URIBuilder(url.get()).build());
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", String.format("Bearer %s", key.get()));
            request.setHeader("OpenAI-Organization", organization.get());
            request.setHeader("OpenAI-Project", project.get());

            var question = OpenAiDataRequest.newRequest(context, message);

            request.setEntity(new StringEntity(JsonSerialization.writeValueAsString(question), ContentType.APPLICATION_JSON));

            try (var response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new RuntimeException(response.getStatusLine().getReasonPhrase());
                }
                var result = JsonSerialization.readValue(response.getEntity().getContent(), clazz);
                logger.debugf("OpenAI response: %s", result);
                return result;
            }
        } catch (URISyntaxException | IOException | RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getResult(String message, Class<T> clazz) {
        return getResult("", message, clazz);
    }

    @Override
    public void close() {

    }
}
