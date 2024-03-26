package org.keycloak.adaptive.spi.policy;

import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.provider.Provider;

public interface UserContextRule extends Provider, ConditionalAuthenticator {
}