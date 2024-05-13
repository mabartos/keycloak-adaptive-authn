package org.keycloak.adaptive.spi.context;

import org.keycloak.provider.Provider;

public abstract class UserContext<T> implements Provider {
    protected T data;
    protected boolean isInitialized;

    public boolean requiresUser() {
        return false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public abstract void initData();

    public T getData() {
        if (!isInitialized()) {
            initData();
        }
        return data;
    }

    @Override
    public void close() {
    }
}
