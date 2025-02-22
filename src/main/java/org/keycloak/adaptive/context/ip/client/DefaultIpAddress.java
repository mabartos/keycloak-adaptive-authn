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
package org.keycloak.adaptive.context.ip.client;

import inet.ipaddr.IPAddress;
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

/**
 * Context for aggregating all IP address contexts and evaluate them based on their priority
 */
public class DefaultIpAddress extends IpAddressContext {
    private final KeycloakSession session;

    public DefaultIpAddress(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<IPAddress> initData() {
        final var contexts = UserContexts.getSortedContexts(session, IpAddressContext.class, DefaultIpAddressFactory.PROVIDER_ID);

        for (var context : contexts) {
            var data = context.initData();
            if (data.isPresent()) {
                return data;
            }
        }
        return Optional.empty();
    }
}
