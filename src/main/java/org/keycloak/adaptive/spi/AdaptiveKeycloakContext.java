package org.keycloak.adaptive.spi;

import org.keycloak.models.KeycloakContext;

public interface AdaptiveKeycloakContext extends KeycloakContext {

    AdaptiveAuthnContext adaptiveAuthnContext();
}
