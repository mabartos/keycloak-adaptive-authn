package org.keycloak.adaptive.spi.engine;

import org.keycloak.models.AuthenticatorConfigModel;

public interface ConfigurableRequirements {

    boolean requiresUser(AuthenticatorConfigModel config);
}
