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
package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class KcUserRoleContextFactory implements UserContextFactory<UserRoleContext> {
    public static final String PROVIDER_ID = "kc-user-role-risk-factor";

    @Override
    public UserRoleContext create(KeycloakSession session) {
        return new KcUserRoleContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
