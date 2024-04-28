package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

public interface UserContext<T> extends Provider {

    default boolean requiresUser() {
        return false;
    }

    boolean isInitialized();

    void initData();

    T getData();

    default void close() {
    }
}
