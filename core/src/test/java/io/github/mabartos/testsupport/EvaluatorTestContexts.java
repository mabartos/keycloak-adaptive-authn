package io.github.mabartos.testsupport;

import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.location.LocationContext;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.context.user.TypicalAccessTimeContext;
import io.github.mabartos.context.user.TypicalAccessTimeData;
import io.github.mabartos.context.user.UserRoleContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * No-op user context stubs for evaluator construction in unit tests.
 */
final class EvaluatorTestContexts {

    private EvaluatorTestContexts() {
    }

    static StubLoginEventsContext loginEvents() {
        return new StubLoginEventsContext();
    }

    static StubUserRoleContext userRole() {
        return new StubUserRoleContext();
    }

    static StubLocationContext location() {
        return new StubLocationContext();
    }

    static StubDeviceRepresentationContext deviceRepresentation() {
        return new StubDeviceRepresentationContext();
    }

    static StubTypicalAccessTimeContext typicalAccessTime(KeycloakSession session) {
        return new StubTypicalAccessTimeContext(session);
    }

    static StubIpAddressContext ipAddress() {
        return new StubIpAddressContext();
    }

    static final class StubLoginEventsContext extends LoginEventsContext {
        StubLoginEventsContext() {
            super(null);
        }

        @Override
        public EventType[] eventTypes() {
            return new EventType[0];
        }

        @Override
        public Optional<List<Event>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
            return Optional.empty();
        }
    }

    static final class StubUserRoleContext extends UserRoleContext {
        StubUserRoleContext() {
            super(null);
        }

        @Override
        public Optional<Set<RoleModel>> initData(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
            return Optional.empty();
        }
    }

    static final class StubLocationContext extends LocationContext {
        StubLocationContext() {
            super(null);
        }

        @Override
        public Optional<LocationData> initData(@Nonnull RealmModel realm) {
            return Optional.empty();
        }
    }

    static final class StubDeviceRepresentationContext extends DeviceRepresentationContext {
        StubDeviceRepresentationContext() {
            super(null);
        }

        @Override
        public Optional<DeviceRepresentation> initData(@Nonnull RealmModel realm) {
            return Optional.empty();
        }
    }

    static final class StubTypicalAccessTimeContext extends TypicalAccessTimeContext {
        StubTypicalAccessTimeContext(KeycloakSession session) {
            super(session);
        }

        @Override
        public Optional<TypicalAccessTimeData> initData(@Nonnull RealmModel realm, @Nullable UserModel user) {
            return Optional.empty();
        }
    }

    static final class StubIpAddressContext extends IpAddressContext {
        StubIpAddressContext() {
            super(null);
        }

        @Override
        public Optional<IPAddress> initData(@Nonnull RealmModel realm) {
            return Optional.empty();
        }
    }
}
