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

package org.keycloak.adaptive.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Default received data from common AI NLP engines (OpenAI ChatGPT, IBM Granite)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DefaultAiDataResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(String finish_reason, Choice.Message message) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Message(String role, String content) {
        }
    }
}