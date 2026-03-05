package io.github.mabartos.context.location;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class KnownLocationContextFactory implements UserContextFactory<KnownLocationContext> {
    public static final String PROVIDER_ID = "known-location-context";

    @Override
    public KnownLocationContext create(KeycloakSession session) {
        return new KnownLocationContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
