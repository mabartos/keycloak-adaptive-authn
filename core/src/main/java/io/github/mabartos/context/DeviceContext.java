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

import io.github.mabartos.spi.context.AbstractUserContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

/**
 * Context related to device itself not requiring known user - representation, IP, user agent,...
 */
public abstract class DeviceContext<T> extends AbstractUserContext<T> {

    public DeviceContext(KeycloakSession session) {
        super(session);
    }

    public abstract Optional<T> initData(@Nonnull RealmModel realm);

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean alwaysFetch() {
        return false;
    }

    @Override
    public Optional<T> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        return initData(realm);
    }
}
