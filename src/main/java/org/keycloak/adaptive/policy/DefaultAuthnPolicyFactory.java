package org.keycloak.adaptive.policy;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    public static final String PROVIDER_ID = "default-authn-policy";

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
}
