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
import java.util.List;

public class UserContexts {

    /**
     * Retrieve user context with the subtype {@param type} with the highest priority
     *
     * @param session            Keycloak session
     * @param type               UserContext type
     * @return user context with the highest priority for its {@link UserContextFactory#getUserContextClass()}
     * @throws IllegalStateException if no provider is found
     */
    @Nonnull
    public static <T extends UserContext<?>> T getContext(@Nonnull KeycloakSession session, @Nonnull Class<T> type) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(UserContext.class)
                .map(f -> (UserContextFactory<?>) f)
                .filter(f -> f.getUserContextClass().equals(type))
                .max(Comparator.comparingInt(UserContextFactory::getPriority))
                .map(f -> (T) f.create(session))
                .orElseThrow(() -> new IllegalStateException("Cannot find any provider with the type '%s'".formatted(type.getSimpleName())));
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
