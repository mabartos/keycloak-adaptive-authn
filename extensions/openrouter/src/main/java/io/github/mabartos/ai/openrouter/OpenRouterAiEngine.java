package io.github.mabartos.ai.openrouter;

import io.github.mabartos.ai.AiEngineUtils;
import io.github.mabartos.ai.DefaultAiDataRequest;
import io.github.mabartos.ai.DefaultAiDataResponse;
import io.github.mabartos.spi.ai.AiEngine;
import io.github.mabartos.spi.level.Risk;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Map;
import java.util.Optional;

/**
 * OpenRouter AI engine (OpenAI-compatible chat completions API).
 * Model id is any slug listed at https://openrouter.ai/models.
 */
public class OpenRouterAiEngine implements AiEngine {
    private static final Logger logger = Logger.getLogger(OpenRouterAiEngine.class);

    private final HttpClientProvider httpClientProvider;

    public OpenRouterAiEngine(KeycloakSession session) {
        this.httpClientProvider = session.getProvider(HttpClientProvider.class);
    }

    @Override
    public <T> Optional<T> getResult(String context, String message, Class<T> clazz, DefaultAiDataRequest.ResponseFormat responseFormat) {
        final var key = OpenRouterAiEngineFactory.getApiKey();
        final var model = OpenRouterAiEngineFactory.getModel();

        if (key.isEmpty() || model.isEmpty()) {
            logger.warnf("OpenRouter API key or model is missing (OPENROUTER_API_KEY, OPENROUTER_API_MODEL). Check the OpenRouter extension README. Ignoring result");
            return Optional.empty();
        }

        var result = AiEngineUtils.aiEngineRequest(
                httpClientProvider.getHttpClient(),
                OpenRouterAiEngineFactory.getApiUrl(),
                () -> DefaultAiDataRequest.newRequest(model.get(), context, message, responseFormat),
                Map.of("Authorization", "Bearer %s".formatted(key.get())),
                clazz
        );

        logger.tracef("Response from OpenRouter AI engine: %s", result);
        return result;
    }

    @Override
    public Risk getRisk(String context, String message) {
        var response = getResult(
                context,
                message,
                DefaultAiDataResponse.class,
                DefaultAiDataRequest.newJsonResponseFormat("risk_evaluation", AiEngine.DEFAULT_RISK_SCHEMA)
        );
        if (response.isEmpty()) {
            return Risk.invalid("No response from OpenRouter");
        }

        return AiEngineUtils.getRiskFromDefaultResponse(
                response.get(),
                eval -> logger.tracef("OpenRouter evaluated risk: %s. Reason: %s", eval.risk(), eval.reason())
        );
    }

    @Override
    public void close() {
    }
}
