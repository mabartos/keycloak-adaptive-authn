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
package io.github.mabartos.context.device;

import io.github.mabartos.context.DeviceContext;
import jakarta.annotation.Nonnull;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;

/**
 * Device context obtained from the Keycloak Device representation
 */
public class DeviceRepresentationContext extends DeviceContext<DeviceRepresentation> {
    private final KeycloakSession session;

    public DeviceRepresentationContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<DeviceRepresentation> initData(@Nonnull RealmModel realm) {
        return Optional.ofNullable(session.getProvider(DeviceRepresentationProvider.class).deviceRepresentation());

    }
}
