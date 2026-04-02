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

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;
import java.util.function.Function;

public class IpApiLocationContextFactory implements UserContextFactory<LocationContext> {
    public static final String PROVIDER_ID = "ip-api-location-context";
    private static final String API_TOKEN_PROPERTY = "location.ipapi.token";

    public static final Function<IPAddress, String> SERVICE_URL = ip -> {
        String tokenPart = getApiToken()
                .map(token -> "?token=" + token)
                .orElse("");
        return String.format("https://ipapi.co/%s/json%s", ip.toString(), tokenPart);
    };

    @Override
    public IpApiLocationContext create(KeycloakSession session) {
        return new IpApiLocationContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<LocationContext> getUserContextClass() {
        return LocationContext.class;
    }

    static Optional<String> getApiToken() {
        return Configuration.getOptionalValue(API_TOKEN_PROPERTY)
                .filter(token -> !token.isBlank());
    }
}
