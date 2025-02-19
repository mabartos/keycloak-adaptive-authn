package org.keycloak.adaptive.context.user;

import org.keycloak.adaptive.spi.context.AbstractUserContext;
import org.keycloak.events.Event;

import java.util.List;

public abstract class LoginEventsContext extends AbstractUserContext<List<Event>> {
    public static final String LOGIN_EVENTS = "login-events-user-context";

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean alwaysFetch() {
        return false;
    }
}
