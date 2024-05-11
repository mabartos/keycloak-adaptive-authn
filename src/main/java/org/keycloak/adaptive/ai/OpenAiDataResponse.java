package org.keycloak.adaptive.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
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
        public record Message(String role, Data content) {

            @JsonCreator
            public Message(@JsonProperty("role") String role, @JsonProperty("content") String content) throws IOException {
                this(role, JsonSerialization.readValue(content, Data.class));
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Data(Double risk, String reason) {
            }
        }
    }
}
