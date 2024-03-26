package org.keycloak.adaptive.spi.policy;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;

import java.util.Set;

public interface UserContextRuleFactory<T extends UserContext<?>> extends ConditionalAuthenticatorFactory {

    Set<Operation<T>> getRules();

}
