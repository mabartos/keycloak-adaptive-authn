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
package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Comparator;
import java.util.List;

public class UserContexts {

    /**
     * Retrieve user contexts sorted by their specified priority
     *
     * @param session            Keycloak session
     * @param context            required user context type
     * @param excludedProviderId excluded providerId from the retrieval
     * @param <T>                user context type
     * @return list of user contexts
     */
    public static <T extends UserContext<?>> List<T> getSortedContexts(KeycloakSession session, Class<T> context, String excludedProviderId) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(UserContext.class)
                .filter(f -> !f.getId().equals(excludedProviderId))
                .map(f -> f.create(session))
                .filter(f -> context.isAssignableFrom(f.getClass()))
                .map(f -> (T) f)
                .sorted(Comparator.comparingInt(f -> ((UserContext<?>) f).getPriority()).reversed())
                .toList();
    }

    /**
     * Retrieve required context based on the providerId
     *
     * @param session    Keycloak session
     * @param providerId user context provider ID
     * @return user context
     */
    public static <T extends UserContext<?>> T getContext(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContext.class, providerId);
    }

    /**
     * Retrieve required context condition based on the providerId
     *
     * @param session    Keycloak session
     * @param providerId user context provider ID
     * @return user context condition
     */
    public static <T extends UserContextCondition> T getContextCondition(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContextCondition.class, providerId);
    }


}
