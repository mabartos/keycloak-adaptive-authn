package org.keycloak.adaptive.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiDataResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(String finish_reason, Message message) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Message(String role, String content) {
        }
    }
}