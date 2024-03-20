package org.keycloak.adaptive;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.AdaptiveAuthnFactory;
import org.keycloak.adaptive.spi.AdaptiveAuthnProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultAdaptiveAuthnFactory implements AdaptiveAuthnFactory {
    public static final String PROVIDER_ID = "default-adaptive-authn";
    public static final AdaptiveAuthnProvider SINGLETON = new DefaultAdaptiveAuthnProvider();

    @Override
    public AdaptiveAuthnProvider create(KeycloakSession session) {
        return SINGLETON;
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
