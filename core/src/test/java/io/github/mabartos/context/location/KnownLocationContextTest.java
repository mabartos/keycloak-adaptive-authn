package io.github.mabartos.context.location;

import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.context.UserContextFactory;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

class KnownLocationContextTest {

    @Test
    void initData_appliesLegacyBackfillInMemoryWithoutPersisting() {
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);

        var context = new KnownLocationContext(null);
        var result = context.initData(realm, user);

        assertThat(result.orElseThrow().size(), is(1));
        assertThat(attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst(), is("France:Paris"));
    }

    @Test
    void onSuccessfulLogin_backfillsLegacyEntries() {
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);
        var session = sessionWithLocation(new StaticLocationContext("France", "Paris"));

        int loginTime = Time.currentTime();
        new KnownLocationContext(session).onSuccessfulLogin(realm, user);

        var parsed = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst());
        assertThat(parsed.getCountry(), is("France"));
        assertThat(parsed.getCity(), is("Paris"));
        assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
    }

    @Test
    void initData_ignoresExpiredEntriesWithoutPersisting() {
        int stale = Time.currentTime() - (int) Duration.ofDays(120).toSeconds();
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);

        var context = new KnownLocationContext(null);
        var result = context.initData(realm, user);

        assertThat(result.isEmpty(), is(true));
        assertThat(attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst(), is("France:Paris:" + stale));
    }

    @Test
    void keepsFreshEntriesOnRead() {
        int fresh = Time.currentTime() - (int) Duration.ofDays(10).toSeconds();
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + fresh));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);

        var context = new KnownLocationContext(null);
        var result = context.initData(realm, user);

        assertThat(result.orElseThrow().size(), is(1));
    }

    @Test
    void getTtlDays_fallsBackToDefaultWhenRealmAttributeInvalid() {
        var realm = (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class[]{RealmModel.class},
                (proxy, method, args) -> "getAttribute".equals(method.getName())
                        ? "not-a-number"
                        : null
        );

        assertThat(KnownLocationContext.getTtlDays(realm), is(KnownLocationContext.DEFAULT_TTL_DAYS));
    }

    @Test
    void onSuccessfulLogin_removesExpiredEntries() {
        int stale = Time.currentTime() - (int) Duration.ofDays(120).toSeconds();
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);
        var session = sessionWithLocation(new StaticLocationContext("Germany", "Berlin"));

        new KnownLocationContext(session).onSuccessfulLogin(realm, user);

        assertThat(attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).size(), is(1));
        var parsed = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst());
        assertThat(parsed.getCountry(), is("Germany"));
        assertThat(parsed.getCity(), is("Berlin"));
    }

    @Test
    void onSuccessfulLogin_refreshesTimestampForMatchingLocation() {
        int stale = Time.currentTime() - (int) Duration.ofDays(30).toSeconds();
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris:" + stale));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);
        var session = sessionWithLocation(new StaticLocationContext("France", "Paris"));

        int loginTime = Time.currentTime();
        new KnownLocationContext(session).onSuccessfulLogin(realm, user);

        var parsed = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst());
        assertThat(parsed, notNullValue());
        assertThat(parsed.getCountry(), is("France"));
        assertThat(parsed.getCity(), is("Paris"));
        assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
        assertThat(parsed.lastSeenEpochSeconds() > stale, is(true));
    }

    @Test
    void onSuccessfulLogin_doesNotWriteZeroTimestampForLegacyEntries() {
        var attributes = new HashMap<String, List<String>>();
        attributes.put(KnownLocationContext.KNOWN_LOCATIONS_ATTR, List.of("France:Paris"));
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);
        var session = sessionWithLocation(new StaticLocationContext("Germany", "Berlin"));

        int loginTime = Time.currentTime();
        new KnownLocationContext(session).onSuccessfulLogin(realm, user);

        assertThat(attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).size(), is(2));
        var paris = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst());
        assertThat(paris.getCountry(), is("France"));
        assertThat(paris.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
        var berlin = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).get(1));
        assertThat(berlin.getCountry(), is("Germany"));
        assertThat(berlin.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
    }

    @Test
    void onSuccessfulLogin_addsNewLocationWithCurrentTimestamp() {
        var attributes = new HashMap<String, List<String>>();
        var user = userWithAttributes(attributes);
        var realm = realmWithTtl(90);
        var session = sessionWithLocation(new StaticLocationContext("Germany", "Berlin"));

        int loginTime = Time.currentTime();
        new KnownLocationContext(session).onSuccessfulLogin(realm, user);

        assertThat(attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).size(), is(1));
        var parsed = KnownLocationData.parseFromAttribute(
                attributes.get(KnownLocationContext.KNOWN_LOCATIONS_ATTR).getFirst());
        assertThat(parsed.getCountry(), is("Germany"));
        assertThat(parsed.getCity(), is("Berlin"));
        assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
    }

    private static UserModel userWithAttributes(Map<String, List<String>> attributes) {
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttributeStream" -> attributes.getOrDefault(args[0], List.of()).stream();
                    case "setAttribute" -> {
                        attributes.put((String) args[0], new ArrayList<>((List<String>) args[1]));
                        yield null;
                    }
                    default -> null;
                }
        );
    }

    private static RealmModel realmWithTtl(int ttlDays) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class[]{RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> method.getParameterCount() == 1
                            && KnownLocationContext.TTL_DAYS_CONFIG.equals(args[0])
                            ? Integer.toString(ttlDays)
                            : null;
                    default -> null;
                }
        );
    }

    private static KeycloakSession sessionWithLocation(LocationContext locationContext) {
        var contextFactory = (UserContextFactory<?>) Proxy.newProxyInstance(
                UserContextFactory.class.getClassLoader(),
                new Class[]{UserContextFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUserContextClass" -> LocationContext.class;
                    case "getPriority" -> 0;
                    case "getId" -> "test-location-context";
                    default -> null;
                }
        );

        var sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class[]{KeycloakSessionFactory.class},
                (proxy, method, args) -> "getProviderFactoriesStream".equals(method.getName())
                        ? Stream.of(contextFactory)
                        : null
        );

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class[]{KeycloakSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKeycloakSessionFactory" -> sessionFactory;
                    case "getProvider" -> args[0] == UserContext.class ? locationContext : null;
                    default -> null;
                }
        );
    }

    /**
     * {@link LocationContext} stub wired through {@link io.github.mabartos.context.UserContexts}.
     */
    static class StaticLocationContext extends LocationContext {

        private final LocationData data;

        StaticLocationContext(String country, String city) {
            super(null);
            this.data = KnownLocationData.of(country, city, 0);
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public Optional<LocationData> initData(RealmModel realm) {
            return Optional.of(data);
        }

        @Override
        public Optional<LocationData> getData(RealmModel realm) {
            return Optional.of(data);
        }

        @Override
        public Optional<LocationData> getData(RealmModel realm, UserModel knownUser) {
            return Optional.of(data);
        }
    }
}
