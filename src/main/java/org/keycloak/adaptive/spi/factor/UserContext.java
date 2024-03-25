package org.keycloak.adaptive.spi.factor;

import org.keycloak.adaptive.spi.policy.UserContextRule;
import org.keycloak.provider.Provider;

import java.util.function.Predicate;

public interface UserContext<T> extends Provider, UserContextRule<T> {

    boolean isDataInitialized();

    void initData();

    T getData();

    default boolean matchesCondition(Predicate<T> predicate) {
        if (!isDataInitialized()) initData();
        return predicate.test(getData());
    }

    default void close() {
    }
}
