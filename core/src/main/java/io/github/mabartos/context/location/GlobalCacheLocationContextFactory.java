package io.github.mabartos.context.location;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class GlobalCacheLocationContextFactory implements UserContextFactory<LocationContext> {
    public static final String PROVIDER_ID = "global-cache-location-context";

    @Override
    public GlobalCacheLocationContext create(KeycloakSession session) {
        return new GlobalCacheLocationContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<LocationContext> getUserContextClass() {
        return LocationContext.class;
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
