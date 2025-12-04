package io.github.mabartos.context.user;

import io.github.mabartos.spi.context.AbstractUserContext;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;

import java.util.List;

public abstract class LoginEventsContext extends AbstractUserContext<List<Event>> {
    public static final String LOGIN_EVENTS = "login-events-user-context";
    public static final String LOGIN_FAILURES_EVENTS = "login-events-user-context";
    public static final int MAX_EVENTS_COUNT = 60;

    public abstract EventType[] eventTypes();

    public int getMaxEventsCount() {
        return MAX_EVENTS_COUNT;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean alwaysFetch() {
        return false;
    }
}
