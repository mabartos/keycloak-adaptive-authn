package org.keycloak.adaptive.context.user;

import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.Optional;

public class KcLoginEventsContext extends LoginEventsContext {
    private final KeycloakSession session;

    public KcLoginEventsContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public EventType[] eventTypes() {
        return new EventType[]{EventType.LOGIN};
    }

    @Override
    public Optional<List<Event>> initData() {
        var realm = session.getContext().getRealm();
        var user = Optional.ofNullable(session.getContext().getAuthenticationSession()).map(AuthenticationSessionModel::getAuthenticatedUser);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(session.getProvider(EventStoreProvider.class)
                .createQuery()
                .realm(realm.getId())
                .user(user.get().getId())
                .type(eventTypes())
                .maxResults(getMaxEventsCount())
                .orderByDescTime()
                .getResultStream()
                .toList());
    }
}
