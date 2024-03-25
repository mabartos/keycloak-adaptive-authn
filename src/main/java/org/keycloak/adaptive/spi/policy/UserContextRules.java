package org.keycloak.adaptive.spi.policy;

import org.keycloak.provider.Provider;

import java.util.Collection;
import java.util.Map;

public interface UserContextRules<T, Value> extends Provider {

    boolean matchesConditions(Map<String, Value> values);

    Collection<UserContextRule<T>> getRules();
}
