package io.github.mabartos.context.user;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class KcLoginFailuresEventsContextFactory implements UserContextFactory<LoginEventsContext> {
    public static final String PROVIDER_ID = LoginEventsContext.LOGIN_FAILURES_EVENTS;

    @Override
    public LoginEventsContext create(KeycloakSession session) {
        return new KcLoginFailuresEventsContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}