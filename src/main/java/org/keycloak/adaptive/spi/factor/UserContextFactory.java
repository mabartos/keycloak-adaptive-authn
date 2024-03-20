package org.keycloak.adaptive.spi.factor;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface UserContextFactory<T extends Provider> extends ProviderFactory<T> {
}
