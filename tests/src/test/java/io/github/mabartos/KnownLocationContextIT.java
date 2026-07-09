package io.github.mabartos;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.location.KnownLocationContext;
import io.github.mabartos.context.location.KnownLocationData;
import io.github.mabartos.context.location.LocationContext;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@KeycloakIntegrationTest(config = KnownLocationContextIT.Config.class)
class KnownLocationContextIT {

    private static final String AUTH_NOTE_LAST_IP = "adaptive-location-lastIp";
    private static final String AUTH_NOTE_LAST_LOCATION = "adaptive-location-lastData";

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void initData_appliesLegacyBackfillInMemoryWithoutPersisting() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));

                var context = knownLocationContext(session);
                var result = context.initData(realm, user);

                assertThat(result.orElseThrow().size(), is(1));
                assertThat(user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).findFirst().orElseThrow(),
                        is("France:Paris"));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void onSuccessfulLogin_backfillsLegacyEntries() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));

                long loginTime = Time.currentTime();
                withCachedLocation(session, realm, "France", "Paris", () ->
                        knownLocationContext(session).onSuccessfulLogin(realm, user));

                var parsed = KnownLocationData.parseFromAttribute(
                        user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).findFirst().orElseThrow());
                assertThat(parsed.getCountry(), is("France"));
                assertThat(parsed.getCity(), is("Paris"));
                assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void initData_ignoresExpiredEntriesWithoutPersisting() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                long stale = Time.currentTime() - Duration.ofDays(120).toSeconds();
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));

                var context = knownLocationContext(session);
                var result = context.initData(realm, user);

                assertThat(result.isEmpty(), is(true));
                assertThat(user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).findFirst().orElseThrow(),
                        is("France:Paris:" + stale));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void keepsFreshEntriesOnRead() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                long fresh = Time.currentTime() - Duration.ofDays(10).toSeconds();
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + fresh));

                var context = knownLocationContext(session);
                var result = context.initData(realm, user);

                assertThat(result.orElseThrow().size(), is(1));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void getTtlDays_fallsBackToDefaultWhenRealmAttributeInvalid() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realm.setAttribute(KnownLocationContext.TTL_DAYS_CONFIG, "not-a-number");
                assertThat(KnownLocationContext.getTtlDays(realm), is(KnownLocationContext.DEFAULT_TTL_DAYS));
            } finally {
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void onSuccessfulLogin_removesExpiredEntries() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                long stale = Time.currentTime() - Duration.ofDays(120).toSeconds();
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));

                withCachedLocation(session, realm, "Germany", "Berlin", () ->
                        knownLocationContext(session).onSuccessfulLogin(realm, user));

                var stored = user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).toList();
                assertThat(stored.size(), is(1));
                var parsed = KnownLocationData.parseFromAttribute(stored.getFirst());
                assertThat(parsed.getCountry(), is("Germany"));
                assertThat(parsed.getCity(), is("Berlin"));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void onSuccessfulLogin_refreshesTimestampForMatchingLocation() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                long stale = Time.currentTime() - Duration.ofDays(30).toSeconds();
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));

                long loginTime = Time.currentTime();
                withCachedLocation(session, realm, "France", "Paris", () ->
                        knownLocationContext(session).onSuccessfulLogin(realm, user));

                var parsed = KnownLocationData.parseFromAttribute(
                        user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).findFirst().orElseThrow());
                assertThat(parsed, notNullValue());
                assertThat(parsed.getCountry(), is("France"));
                assertThat(parsed.getCity(), is("Paris"));
                assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
                assertThat(parsed.lastSeenEpochSeconds() > stale, is(true));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void onSuccessfulLogin_doesNotWriteZeroTimestampForLegacyEntries() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));

                long loginTime = Time.currentTime();
                withCachedLocation(session, realm, "Germany", "Berlin", () ->
                        knownLocationContext(session).onSuccessfulLogin(realm, user));

                var stored = user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).toList();
                assertThat(stored.size(), is(2));
                var paris = KnownLocationData.parseFromAttribute(stored.get(0));
                assertThat(paris.getCountry(), is("France"));
                assertThat(paris.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
                var berlin = KnownLocationData.parseFromAttribute(stored.get(1));
                assertThat(berlin.getCountry(), is("Germany"));
                assertThat(berlin.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    @Test
    void onSuccessfulLogin_addsNewLocationWithCurrentTimestamp() {
        runOnServer.run(session -> {
            RealmModel realm = adaptiveRealm(session);
            UserModel user = requireUser(session, realm);
            var userSnapshot = UserSnapshot.capture(user);
            var realmSnapshot = RealmSnapshot.capture(realm);
            try {
                realmSnapshot.setTtl(realm, 90);
                user.removeAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR);

                long loginTime = Time.currentTime();
                withCachedLocation(session, realm, "Germany", "Berlin", () ->
                        knownLocationContext(session).onSuccessfulLogin(realm, user));

                var stored = user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).toList();
                assertThat(stored.size(), is(1));
                var parsed = KnownLocationData.parseFromAttribute(stored.getFirst());
                assertThat(parsed.getCountry(), is("Germany"));
                assertThat(parsed.getCity(), is("Berlin"));
                assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
            } finally {
                userSnapshot.restore(user);
                realmSnapshot.restore(realm);
            }
        });
    }

    private static RealmModel adaptiveRealm(KeycloakSession session) {
        return session.realms().getRealmByName("adaptive");
    }

    private static UserModel requireUser(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, "user");
        assertThat("Expected test user in adaptive realm", user != null, is(true));
        return user;
    }

    private static KnownLocationContext knownLocationContext(KeycloakSession session) {
        return UserContexts.getContext(session, KnownLocationContext.class);
    }

    private static void withCachedLocation(
            KeycloakSession session, RealmModel realm, String country, String city, Runnable action) {
        ClientModel client = realm.getClientByClientId("account");
        assertThat("Expected account client in adaptive realm", client != null, is(true));
        RootAuthenticationSessionModel root = session.authenticationSessions().createRootAuthenticationSession(realm);
        try {
            AuthenticationSessionModel authSession = root.createAuthenticationSession(client);
            session.getContext().setRealm(realm);
            session.getContext().setAuthenticationSession(authSession);

            var ip = UserContexts.getContext(session, IpAddressContext.class).getData(realm).orElseThrow();
            authSession.setAuthNote(AUTH_NOTE_LAST_IP, ip.toString());
            authSession.setAuthNote(AUTH_NOTE_LAST_LOCATION, country + ":" + city);

            // Prime the chained LocationContext cache for onSuccessfulLogin.
            UserContexts.getContext(session, LocationContext.class).getData(realm, null);

            action.run();
        } finally {
            session.authenticationSessions().removeRootAuthenticationSession(realm, root);
        }
    }

    private static final class UserSnapshot {
        private final List<String> knownLocations;

        private UserSnapshot(List<String> knownLocations) {
            this.knownLocations = knownLocations;
        }

        static UserSnapshot capture(UserModel user) {
            return new UserSnapshot(
                    user.getAttributeStream(KnownLocationContext.KNOWN_LOCATIONS_ATTR).toList());
        }

        void restore(UserModel user) {
            if (knownLocations.isEmpty()) {
                user.removeAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR);
            } else {
                user.setAttribute(KnownLocationContext.KNOWN_LOCATIONS_ATTR, knownLocations);
            }
        }
    }

    private static final class RealmSnapshot {
        private final String ttlAttribute;

        private RealmSnapshot(String ttlAttribute) {
            this.ttlAttribute = ttlAttribute;
        }

        static RealmSnapshot capture(RealmModel realm) {
            return new RealmSnapshot(realm.getAttribute(KnownLocationContext.TTL_DAYS_CONFIG));
        }

        void setTtl(RealmModel realm, int ttlDays) {
            realm.setAttribute(KnownLocationContext.TTL_DAYS_CONFIG, Integer.toString(ttlDays));
        }

        void restore(RealmModel realm) {
            if (ttlAttribute == null) {
                realm.removeAttribute(KnownLocationContext.TTL_DAYS_CONFIG);
            } else {
                realm.setAttribute(KnownLocationContext.TTL_DAYS_CONFIG, ttlAttribute);
            }
        }
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .option("features", "declarative-ui");
        }
    }
}
