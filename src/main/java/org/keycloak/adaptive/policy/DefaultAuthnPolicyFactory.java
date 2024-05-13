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
package org.keycloak.adaptive.policy;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import static org.keycloak.authentication.AuthenticationFlow.BASIC_FLOW;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    public static final String PROVIDER_ID = "default";
    public static final String DEFAULT_AUTHN_POLICIES_FLOW_ALIAS = "Authentication policies - PARENT";

    @Override
    public AuthnPolicyProvider create(KeycloakSession session) {
        return new DefaultAuthnPolicyProvider(session);
    }

    @Override
    public AuthnPolicyProvider create(KeycloakSession session, RealmModel realm) {
        return new DefaultAuthnPolicyProvider(session, realm);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    static AuthenticationFlowModel createParentFlow(RealmModel realm) {
        var parent = new AuthenticationFlowModel();
        parent.setAlias(DEFAULT_AUTHN_POLICIES_FLOW_ALIAS);
        parent.setDescription("Parent Authentication Policy");
        parent.setProviderId(BASIC_FLOW);
        parent.setTopLevel(true);
        parent.setBuiltIn(false);
        return realm.addAuthenticationFlow(parent);
    }
}
