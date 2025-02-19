package org.keycloak.adaptive.context.user;

import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

public class KcLoginFailuresEventsContext extends KcLoginEventsContext {
    public KcLoginFailuresEventsContext(KeycloakSession session) {
        super(session);
    }

    public EventType[] eventTypes() {
        return new EventType[]{EventType.LOGIN_ERROR};
    }

}
