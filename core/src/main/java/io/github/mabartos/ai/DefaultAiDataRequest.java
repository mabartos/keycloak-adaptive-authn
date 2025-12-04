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

package io.github.mabartos.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Data request for common AI NLP engines (OpenAI ChatGPT, IBM Granite)
 */
public record DefaultAiDataRequest(String model,
                                   List<Message> messages,
                                   @JsonProperty("response_format")
                                   ResponseFormat format,
                                   Double temperature) {

    // Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.
    public static final Double DEFAULT_TEMPERATURE = 0.2;
    public record Message(String role,
                          String content) {
    }

    public record ResponseFormat(String type,
                                 @JsonProperty("json_schema")
                                 JsonSchema jsonSchema) {
        public record JsonSchema(String name,
                                 Schema schema,
                                 Boolean strict) {
            public record Schema(String type,
                                 Boolean additionalProperties,
                                 Map<String, SchemaType> properties,
                                 @JsonProperty("required")
                                 List<String> requiredProperties) {
            }
        }
    }

    public record SchemaType(String type, String description) {
    }

    public static ResponseFormat newJsonResponseFormat(String schemaName, Map<String, SchemaType> properties) {
        return new ResponseFormat("json_schema",
                new ResponseFormat.JsonSchema(schemaName,
                        new ResponseFormat.JsonSchema.Schema("object", false, properties, properties.keySet().stream().toList()),
                        true));
    }

    public static ResponseFormat newTextResponseFormat() {
        return new ResponseFormat("text", null);
    }

    public static DefaultAiDataRequest newRequest(String model, String systemMessage, String userMessage, ResponseFormat format) {
        return new DefaultAiDataRequest(model, List.of(new DefaultAiDataRequest.Message("developer", systemMessage), new DefaultAiDataRequest.Message("user", userMessage)), format, DEFAULT_TEMPERATURE);
    }

    public static DefaultAiDataRequest newRequest(String model, String systemMessage, String userMessage) {
        return newRequest(model, systemMessage, userMessage, null);
    }

    public static DefaultAiDataRequest newRequest(String model, String userMessage) {
        return newRequest(model, "", userMessage);
    }
}
