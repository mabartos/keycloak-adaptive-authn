package org.keycloak.adaptive.context.user;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

import static org.keycloak.adaptive.context.user.LoginEventsContext.LOGIN_EVENTS;

public class KcLoginEventsContextFactory implements UserContextFactory<LoginEventsContext> {
    public static final String PROVIDER_ID = LOGIN_EVENTS;

    @Override
    public LoginEventsContext create(KeycloakSession session) {
        return new KcLoginEventsContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
