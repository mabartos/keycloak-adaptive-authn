package org.keycloak.adaptive.spi.factor;

import java.util.function.Predicate;

public interface UserContextRule<T> {

    boolean matchesCondition(Predicate<T> predicate);
}
