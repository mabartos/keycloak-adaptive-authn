package org.keycloak.adaptive.spi.ai;

import org.keycloak.provider.Provider;

/**
 * Artificial Intelligence Natural Language Processing engine
 */
public interface AiNlpEngine extends Provider {

    <T> T getResult(String message, Class<T> clazz);

    <T> T getResult(String context, String message, Class<T> clazz);
}
