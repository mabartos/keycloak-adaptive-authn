package io.github.mabartos.context.user;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Factory for creating TypicalAccessTimeContext instances.
 */
public class TypicalAccessTimeContextFactory implements UserContextFactory<TypicalAccessTimeContext> {
    public static final String PROVIDER_ID = TypicalAccessTimeContext.PROVIDER_ID;

    @Override
    public TypicalAccessTimeContext create(KeycloakSession session) {
        return new TypicalAccessTimeContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<TypicalAccessTimeContext> getUserContextClass() {
        return TypicalAccessTimeContext.class;
    }
}
