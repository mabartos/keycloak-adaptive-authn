package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

public interface UserContext<T> extends Provider {

    boolean isDataInitialized();

    void initData();

    T getData();

    default void close() {
    }
}
