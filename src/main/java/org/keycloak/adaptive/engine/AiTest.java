package org.keycloak.adaptive.engine;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.CreatedAware;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@CreatedAware
@SystemMessage("This is a test.")
public interface AiTest {

    @UserMessage("Tell me a joke about this word: {word}")
    String test(String word);
}