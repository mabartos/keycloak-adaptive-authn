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

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

import static org.keycloak.adaptive.context.ip.IpAddressUtils.FORWARDED_FOR_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.IP_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.getIpAddressFromHeader;

public class HeaderIpAddressContext extends IpAddressContext {
    private final KeycloakSession session;

    public HeaderIpAddressContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void initData() {
        var ipAddress = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRequestHeaders)
                .map(headers -> getIpAddressFromHeader(headers, "Forwarded", FORWARDED_FOR_PATTERN)
                        .or(() -> getIpAddressFromHeader(headers, "X-Forwarded-For", IP_PATTERN)))
                .filter(Optional::isPresent)
                .map(Optional::get);

        if (ipAddress.isPresent()) {
            this.data = ipAddress.get();
            this.isInitialized = true;
        } else {
            this.isInitialized = false;
        }
    }
}
