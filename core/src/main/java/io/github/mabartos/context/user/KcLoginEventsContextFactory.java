package io.github.mabartos.context.user;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class KcLoginEventsContextFactory implements UserContextFactory<LoginEventsContext> {
    public static final String PROVIDER_ID = LoginEventsContext.LOGIN_EVENTS;

    @Override
    public LoginEventsContext create(KeycloakSession session) {
        return new KcLoginEventsContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
