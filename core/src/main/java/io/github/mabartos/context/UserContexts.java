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
package io.github.mabartos.context;

import io.github.mabartos.spi.condition.UserContextCondition;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.context.UserContextFactory;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class UserContexts {

    /**
     * Retrieve chained user context with the subtype {@param type}.
     * Returns a context that register its delegates.
     * If the context cannot obtain data, it calls its delegates and tries to obtain it in different way
     *
     * @param session Keycloak session
     * @param type    UserContext type
     * @throws IllegalStateException if no provider is found
     */
    @Nonnull
    public static <T extends UserContext<?>> T getContext(@Nonnull KeycloakSession session, @Nonnull Class<T> type) {
        LinkedList<T> contexts = session.getKeycloakSessionFactory().getProviderFactoriesStream(UserContext.class)
                .map(f -> (UserContextFactory<?>) f)
                .filter(f -> f.getUserContextClass().equals(type))
                .sorted(Comparator.comparingInt(f -> ((UserContextFactory<?>) f).getPriority()).reversed())
                .map(f -> (T) session.getProvider(UserContext.class, f.getId()))
                .collect(Collectors.toCollection(LinkedList::new));

        if (contexts.isEmpty()) {
            throw new IllegalStateException("Cannot find any provider with the type '%s'".formatted(type.getSimpleName()));
        }

        // If there's only one context, return it directly without chaining
        if (contexts.size() == 1) {
            return contexts.getFirst();
        }

        // Chain contexts together using delegation
        // First context delegates to second, second to third, etc.
        for (int i = 0; i < contexts.size() - 1; i++) {
            T current = contexts.get(i);
            T next = contexts.get(i + 1);
            current.setDelegate(next);
        }

        return contexts.getFirst();
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
