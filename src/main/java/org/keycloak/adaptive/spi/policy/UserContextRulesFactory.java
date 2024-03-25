package org.keycloak.adaptive.spi.policy;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.provider.ProviderFactory;

public interface UserContextRulesFactory<T extends UserContext<T>> extends ProviderFactory<UserContextRules<T>>, ConfiguredRuleProvider {
}
