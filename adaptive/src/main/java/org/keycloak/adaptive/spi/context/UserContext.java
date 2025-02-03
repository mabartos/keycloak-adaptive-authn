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
package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

/**
 * Data representing information about characteristics of the authentication attempt.
 * Abstraction for declarative retrieval of specific authentication factors as we do not really care about the approach how the data was obtained.
 * Data is retrieved only once for the request as it is cached.
 * <p></p>
 * Mainly used for aggregating data for risk-based authentication.
 * Can be information about user, device, location, etc.
 *
 * @param <T> specific data to retrieve
 */
public abstract class UserContext<T> implements Provider {
    protected T data;
    protected boolean isInitialized;

    /**
     * Flag to determine whether we need a basic information about the authentication user
     *
     * @return true if the basic info is needed
     */
    public boolean requiresUser() {
        return false;
    }

    /**
     * Priority of the user context used for ordering when multiple implementation of the same user context is present
     * The higher the priority is, the sooner the user context is retrieved in sorted ops.
     *
     * @return priority
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Flag to determine the data was correctly obtained
     *
     * @return true if data is present, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Initialize the process of obtaining the required data
     */
    public abstract void initData();

    /**
     * Get the specific user context data.
     * If the data are not properly initialized, it retries the retrieval
     *
     * @return specific data
     */
    public T getData() {
        if (!isInitialized()) {
            initData();
        }
        return data;
    }

    @Override
    public void close() {
    }
}
