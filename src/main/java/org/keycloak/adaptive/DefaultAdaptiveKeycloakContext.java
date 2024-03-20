package org.keycloak.adaptive;

import org.keycloak.adaptive.spi.AdaptiveAuthnContext;
import org.keycloak.adaptive.spi.AdaptiveKeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakContext;

public class DefaultAdaptiveKeycloakContext extends DefaultKeycloakContext implements AdaptiveKeycloakContext {
    private AdaptiveAuthnContext adaptiveAuthnContext;

    public DefaultAdaptiveKeycloakContext(KeycloakSession session) {
        super(session);
    }

    @Override
    public AdaptiveAuthnContext adaptiveAuthnContext() {
        if (adaptiveAuthnContext == null) {
            adaptiveAuthnContext = new DefaultAdaptiveAuthnContext(getSession());
        }
        return adaptiveAuthnContext;
    }
}
