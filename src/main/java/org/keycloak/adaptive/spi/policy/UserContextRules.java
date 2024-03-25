package org.keycloak.adaptive.spi.policy;

import org.keycloak.provider.Provider;

import java.util.Collection;

public interface UserContextRules<T extends UserContextRule<T>> extends Provider {

    Collection<T> getRules();
}
