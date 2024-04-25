package org.keycloak.adaptive.ai;

import java.util.List;

public record OpenAiDataRequest(String model,
                                List<Message> messages) {

    public record Message(String role,
                          String content) {
    }

    public static OpenAiDataRequest newRequest(String model, String systemMessage, String userMessage) {
        return new OpenAiDataRequest(model, List.of(new Message("system", systemMessage), new Message("user", userMessage)));
    }

    public static OpenAiDataRequest newRequest(String systemMessage, String userMessage) {
        return newRequest("gpt-3.5-turbo", systemMessage, userMessage);
    }

    public static OpenAiDataRequest newRequest(String userMessage) {
        return newRequest("", userMessage);
    }
}
