package io.github.mabartos.context.user;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KcLoginEventsContext extends LoginEventsContext {
    private final EventStoreProvider eventStore;

    public KcLoginEventsContext(KeycloakSession session) {
        super(session);
        this.eventStore = session.getProvider(EventStoreProvider.class);
    }

    @Override
    public EventType[] eventTypes() {
        return new EventType[]{EventType.LOGIN};
    }

    @Override
    public Optional<List<Event>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        Objects.requireNonNull(knownUser, "User cannot be null");

        return Optional.of(eventStore.createQuery()
                .realm(realm.getId())
                .user(knownUser.getId())
                .type(eventTypes())
                .maxResults(getMaxEventsCount())
                .orderByDescTime()
                .getResultStream()
                .toList());
    }
}
