package org.keycloak.adaptive.spi.policy;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.provider.ProviderFactory;

public interface UserContextRulesFactory<T extends UserContext<?>> extends ProviderFactory<UserContextRules<T, String>>, ConfiguredRuleProvider<T> {
}
