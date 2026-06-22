package io.github.mabartos.ai.openrouter;

import io.github.mabartos.spi.ai.AiEngineFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;

public class OpenRouterAiEngineFactory implements AiEngineFactory {
    public static final String PROVIDER_ID = "openrouter";

    private static final String URL_PROPERTY = "openrouter.api.url";
    private static final String KEY_PROPERTY = "openrouter.api.key";
    private static final String MODEL_PROPERTY = "openrouter.api.model";

    private static final String DEFAULT_URL = "https://openrouter.ai/api/v1/chat/completions";

    @Override
    public OpenRouterAiEngine create(KeycloakSession session) {
        return new OpenRouterAiEngine(session);
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

    public static String getApiUrl() {
        return Configuration.getOptionalValue(URL_PROPERTY)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_URL);
    }

    public static Optional<String> getApiKey() {
        return Configuration.getOptionalValue(KEY_PROPERTY)
                .filter(value -> !value.isBlank());
    }

    public static Optional<String> getModel() {
        return Configuration.getOptionalValue(MODEL_PROPERTY)
                .filter(value -> !value.isBlank());
    }
}
