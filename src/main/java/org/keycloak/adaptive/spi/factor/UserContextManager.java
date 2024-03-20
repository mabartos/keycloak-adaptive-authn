package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.Provider;

import java.util.Collection;

public interface UserContextManager extends Provider {

    void initData();

    Collection<UserContext<?>> getData(Class<UserContext<?>> contextClass);
}
