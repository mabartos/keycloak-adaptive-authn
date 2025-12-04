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
package io.github.mabartos.context.location;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.spi.condition.Operation;
import io.github.mabartos.spi.condition.UserContextCondition;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;

import java.util.List;

/**
 * Condition for checking location properties
 */
public class LocationCondition implements UserContextCondition, ConditionalAuthenticator {
    private final KeycloakSession session;
    private final LocationContext locationContext;
    private final List<Operation<LocationContext>> rules;

    public LocationCondition(KeycloakSession session, List<Operation<LocationContext>> rules) {
        this.session = session;
        this.locationContext = UserContexts.getContext(session, IpApiLocationContextFactory.PROVIDER_ID);
        this.rules = rules;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        // TODO
        return false;
    }
}
