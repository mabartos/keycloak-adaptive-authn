package org.keycloak.adaptive.ai;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.ai.AiEngineFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class OpenAiEngineFactory implements AiEngineFactory {
    public static final String PROVIDER_ID = "default";

    static final String URL_PROPERTY = "openai.api.url";
    static final String KEY_PROPERTY = "openai.api.key";
    static final String ORGANIZATION_PROPERTY = "openai.api.organization";
    static final String PROJECT_PROPERTY = "openai.api.project";

    @Override
    public OpenAiEngine create(KeycloakSession session) {
        return new OpenAiEngine(session);
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
