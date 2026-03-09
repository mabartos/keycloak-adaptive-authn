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

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class AuthnSessionLocationContextFactory implements UserContextFactory<LocationContext> {
    public static final String PROVIDER_ID = "authn-session-location-context";

    @Override
    public AuthnSessionLocationContext create(KeycloakSession session) {
        return new AuthnSessionLocationContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<LocationContext> getUserContextClass() {
        return LocationContext.class;
    }

    @Override
    public int getPriority() {
        // Higher priority than IpApiLocationContext (default is 0)
        // This ensures the session cache is checked first before calling external APIs
        return 100;
    }
}
