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
package io.github.mabartos.spi.condition;

import io.github.mabartos.spi.context.UserContext;

import java.util.List;

/**
 * User context verifiable by the included operations
 *
 * @param <T> user context
 */
public interface VerifiableUserContext<T extends UserContext<?>> {

    /**
     * Initialize user context operations
     *
     * @return list of initialized operations
     */
    List<Operation<T>> initOperations();

    /**
     * Retrieve user context operations
     *
     * @return list of operations
     */
    List<Operation<T>> getOperations();

    /**
     * Retrieve names of operations to be shown to user
     *
     * @return list of operations' names
     */
    List<String> getOperationsTexts();
}
