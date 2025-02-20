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
import org.keycloak.adaptive.context.device.DefaultDeviceContextFactory;
import org.keycloak.adaptive.context.device.DeviceContext;
import org.keycloak.adaptive.context.ip.IpAddressUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;

/**
 * IP address obtained from the {@link DeviceContext}
 */
public class DeviceIpAddressContext extends IpAddressContext {
    private final KeycloakSession session;

    public DeviceIpAddressContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Optional<IPAddress> initData() {
        final DeviceContext deviceContext = UserContexts.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);

        return deviceContext.getData()
                .map(DeviceRepresentation::getIpAddress)
                .flatMap(IpAddressUtils::getIpAddress);
    }
}
