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

import java.util.Optional;

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
public interface UserContext<T> extends Provider {

    /**
     * Flag to determine whether we need a basic information about the authentication user
     *
     * @return true if the basic info is needed
     */
    boolean requiresUser();

    /**
     * Priority of the user context used for ordering when multiple implementation of the same user context is present
     * The higher the priority is, the sooner the user context is retrieved in sorted ops.
     *
     * @return priority
     */
    int getPriority();

    /**
     * Flag to determine if the data should be always re-fetched
     * <p>
     * User contexts from remote locations should be fetched only once per session
     *
     * @return true if {@link UserContext#getData()} should always call {@link UserContext#initData()}
     */
    boolean alwaysFetch();

    /**
     * Flag to determine the data was correctly obtained
     *
     * @return true if data is present, false otherwise
     */
    boolean isInitialized();

    /**
     * Initialize the process of obtaining the required data
     */
    Optional<T> initData();

    /**
     * Get the specific user context data.
     *
     * @return specific data
     */
    Optional<T> getData();
}
