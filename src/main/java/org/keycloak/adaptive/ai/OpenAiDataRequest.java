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
        return newRequest(OpenAiEngineFactory.DEFAULT_MODEL, systemMessage, userMessage);
    }

    public static OpenAiDataRequest newRequest(String userMessage) {
        return newRequest("", userMessage);
    }
}
