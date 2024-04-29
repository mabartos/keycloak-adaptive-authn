package org.keycloak.adaptive.services;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class AuthnPoliciesResourceFactory implements RealmResourceProviderFactory {
    public static final String PROVIDER_ID = "authn-policies";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new AuthnPoliciesResource(session);
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
}
