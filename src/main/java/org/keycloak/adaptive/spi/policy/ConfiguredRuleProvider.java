package org.keycloak.adaptive.spi.policy;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.provider.ConfiguredProvider;

import java.util.Map;

public interface ConfiguredRuleProvider<T extends UserContext<?>> extends ConfiguredProvider {

    Map<String, UserContextRule<T>> getRules();
}
