package io.github.mabartos;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.KnownDeviceConstants;
import io.github.mabartos.context.device.KnownDeviceContext;
import io.github.mabartos.context.device.KnownDeviceData;
import io.github.mabartos.engine.LoginEventsEventListenerFactory;
import io.github.mabartos.evaluator.EvaluatorUtils;
import io.github.mabartos.evaluator.device.DeviceFingerprintCollector;
import io.github.mabartos.evaluator.device.KnownDeviceRiskEvaluator;
import io.github.mabartos.evaluator.device.KnownDeviceRiskEvaluatorFactory;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;
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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the FingerprintJS known device extension on an embedded Keycloak server.
 */
@KeycloakIntegrationTest(config = KnownDeviceIntegrationTest.Config.class)
class KnownDeviceIntegrationTest {

    private static final String VISITOR_ID = "a3f5b2c1d4e5f678901234567890123a";
    private static final String UNKNOWN_VISITOR_ID = "b3f5b2c1d4e5f678901234567890123b";

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.METHOD)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void knownDeviceEvaluatorFactoryRegisteredOnServer() {
        runOnServer.run(session -> {
            var factory = (RiskEvaluatorFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(RiskEvaluator.class, KnownDeviceRiskEvaluatorFactory.PROVIDER_ID);
            assertNotNull(factory, "KnownDeviceRiskEvaluatorFactory should be registered when extension is loaded");
            assertThat(factory.evaluatorClass(), is(KnownDeviceRiskEvaluator.class));
        });
    }

    @Test
    void knownDeviceContextResolvedFromUserContextChain() {
        runOnServer.run(session -> {
            var context = UserContexts.getContext(session, KnownDeviceContext.class);
            assertNotNull(context);
            assertTrue(context instanceof KnownDeviceContext);
        });
    }

    @Test
    void registersKnownDeviceOnLoginAndTrustsItOnSecondEvaluation() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);

            var evaluator = new KnownDeviceRiskEvaluator(session);
            var knownDeviceContext = UserContexts.getContext(session, KnownDeviceContext.class);

            bindVisitorId(session, realm, client, VISITOR_ID);
            assertThat(evaluator.evaluate(realm, user).getScore(), is(VERY_SMALL));

            knownDeviceContext.onSuccessfulLogin(realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertNotNull(stored);
            assertThat(stored.size(), is(1));
            var parsed = KnownDeviceData.parseFromAttribute(stored.getFirst());
            assertNotNull(parsed);
            assertThat(parsed.visitorId(), is(VISITOR_ID));

            bindVisitorId(session, realm, client, VISITOR_ID);
            assertThat(evaluator.evaluate(realm, user).getScore(), is(NEGATIVE_LOW));
        });
    }

    @Test
    void registersKnownDeviceOnLoginEventAfterUserKnownEvaluation() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);

            var evaluator = new KnownDeviceRiskEvaluator(session);
            bindVisitorId(session, realm, client, VISITOR_ID);
            assertThat(evaluator.evaluate(realm, user).getScore(), is(VERY_SMALL));

            session.getContext().setAuthenticationSession(null);
            fireLoginEvent(session, realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertNotNull(stored);
            assertThat(stored.size(), is(1));
            assertThat(KnownDeviceData.parseFromAttribute(stored.getFirst()).visitorId(), is(VISITOR_ID));

            bindVisitorId(session, realm, client, VISITOR_ID);
            assertThat(evaluator.evaluate(realm, user).getScore(), is(NEGATIVE_LOW));
        });
    }

    @Test
    void initDataLegacyBackfillDoesNotPersist() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            prepareEvaluator(session, realm, user);
            setKnownDevices(user, VISITOR_ID);

            var knownDeviceContext = UserContexts.getContext(session, KnownDeviceContext.class);
            assertThat(knownDeviceContext.getData(realm, user).orElseThrow().size(), is(1));
            assertThat(user.getFirstAttribute(KnownDeviceContext.KNOWN_DEVICES_ATTR), is(VISITOR_ID));
        });
    }

    @Test
    void onSuccessfulLoginBackfillsLegacyTimestamp() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            setKnownDevices(user, VISITOR_ID);

            long loginTime = Time.currentTime();
            bindVisitorId(session, realm, client, VISITOR_ID);
            UserContexts.getContext(session, KnownDeviceContext.class).onSuccessfulLogin(realm, user);

            var parsed = KnownDeviceData.parseFromAttribute(
                    user.getFirstAttribute(KnownDeviceContext.KNOWN_DEVICES_ATTR));
            assertNotNull(parsed);
            assertThat(parsed.visitorId(), is(VISITOR_ID));
            assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
        });
    }

    @Test
    void initDataIgnoresExpiredEntriesWithoutPersisting() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            prepareEvaluator(session, realm, user);
            long stale = Time.currentTime() - Duration.ofDays(120).getSeconds();
            String staleAttribute = VISITOR_ID + ":" + stale;
            setKnownDevices(user, staleAttribute);

            var knownDeviceContext = UserContexts.getContext(session, KnownDeviceContext.class);
            assertTrue(knownDeviceContext.getData(realm, user).isEmpty());
            assertThat(user.getFirstAttribute(KnownDeviceContext.KNOWN_DEVICES_ATTR), is(staleAttribute));
        });
    }

    @Test
    void initDataKeepsFreshEntries() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            prepareEvaluator(session, realm, user);
            long fresh = Time.currentTime() - Duration.ofDays(10).getSeconds();
            setKnownDevices(user, VISITOR_ID + ":" + fresh);

            var knownDeviceContext = UserContexts.getContext(session, KnownDeviceContext.class);
            assertThat(knownDeviceContext.getData(realm, user).orElseThrow().size(), is(1));
        });
    }

    @Test
    void getTtlDaysFallsBackWhenRealmAttributeInvalid() {
        runOnServer.run(session -> {
            var realm = realm(session);
            realm.setAttribute(KnownDeviceContext.TTL_DAYS_CONFIG, "not-a-number");
            assertThat(KnownDeviceContext.getTtlDays(realm), is(KnownDeviceContext.DEFAULT_TTL_DAYS));
        });
    }

    @Test
    void getMaxStoredDevicesFallsBackWhenRealmAttributeInvalid() {
        runOnServer.run(session -> {
            var realm = realm(session);
            realm.setAttribute(KnownDeviceContext.MAX_STORED_DEVICES_CONFIG, "not-a-number");
            assertThat(KnownDeviceContext.getMaxStoredDevices(realm), is(KnownDeviceContext.DEFAULT_MAX_STORED_DEVICES));

            realm.setAttribute(KnownDeviceContext.MAX_STORED_DEVICES_CONFIG, "0");
            assertThat(KnownDeviceContext.getMaxStoredDevices(realm), is(KnownDeviceContext.DEFAULT_MAX_STORED_DEVICES));
        });
    }

    @Test
    void onSuccessfulLoginRemovesExpiredAndRegistersCurrent() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            long stale = Time.currentTime() - Duration.ofDays(120).getSeconds();
            setKnownDevices(user, VISITOR_ID + ":" + stale);

            bindVisitorId(session, realm, client, VISITOR_ID);
            UserContexts.getContext(session, KnownDeviceContext.class).onSuccessfulLogin(realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertThat(stored.size(), is(1));
            assertThat(KnownDeviceData.parseFromAttribute(stored.getFirst()).visitorId(), is(VISITOR_ID));
        });
    }

    @Test
    void returnsVerySmallWhenExpiredDeviceNotInActiveSet() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            long stale = Time.currentTime() - Duration.ofDays(120).getSeconds();
            setKnownDevices(user, VISITOR_ID + ":" + stale);

            bindVisitorId(session, realm, client, VISITOR_ID);
            assertThat(new KnownDeviceRiskEvaluator(session).evaluate(realm, user).getScore(), is(VERY_SMALL));
        });
    }

    @Test
    void returnsMediumForUnknownDevice() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            setKnownDevices(user, VISITOR_ID);

            bindVisitorId(session, realm, client, UNKNOWN_VISITOR_ID);
            assertThat(new KnownDeviceRiskEvaluator(session).evaluate(realm, user).getScore(), is(MEDIUM));
        });
    }

    @Test
    void treatsInvalidFingerprintAsUnknownDevice() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            setKnownDevices(user, VISITOR_ID);

            bindVisitorId(session, realm, client, "not-a-valid-fingerprint");
            assertThat(new KnownDeviceRiskEvaluator(session).evaluate(realm, user).getScore(), is(MEDIUM));
        });
    }

    @Test
    void returnsVerySmallWhenFingerprintMissingOnFirstLogin() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);

            bindVisitorId(session, realm, client, null);
            assertThat(new KnownDeviceRiskEvaluator(session).evaluate(realm, user).getScore(), is(VERY_SMALL));
        });
    }

    @Test
    void treatsMissingFingerprintAsUnknownDevice() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            setKnownDevices(user, VISITOR_ID);

            bindVisitorId(session, realm, client, null);
            assertThat(new KnownDeviceRiskEvaluator(session).evaluate(realm, user).getScore(), is(MEDIUM));
        });
    }

    @Test
    void collectorSkipsWhenEvaluatorDisabled() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var client = client(realm);
            EvaluatorUtils.setEvaluatorEnabled(realm, KnownDeviceRiskEvaluator.class, false);

            var authSession = createAuthSession(session, realm, client);
            var flow = new RecordingAuthenticationFlowContext(realm, authSession);
            new DeviceFingerprintCollector().authenticate(flow);

            assertThat(flow.successCalled(), is(true));
            assertThat(flow.challengeCalled(), is(false));
        });
    }

    @Test
    void collectorStoresValidVisitorIdAndSucceeds() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var client = client(realm);
            var authSession = createAuthSession(session, realm, client);
            var flow = new RecordingAuthenticationFlowContext(realm, authSession);
            flow.formParameters().add(KnownDeviceConstants.FORM_PARAM, VISITOR_ID);

            new DeviceFingerprintCollector().action(flow);

            assertThat(flow.successCalled(), is(true));
            assertThat(authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE), is(VISITOR_ID));
        });
    }

    @Test
    void collectorRejectsInvalidVisitorIdAndSucceeds() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var client = client(realm);
            var authSession = createAuthSession(session, realm, client);
            authSession.setAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE, VISITOR_ID);
            var flow = new RecordingAuthenticationFlowContext(realm, authSession);
            flow.formParameters().add(KnownDeviceConstants.FORM_PARAM, "not-a-valid-fingerprint");

            new DeviceFingerprintCollector().action(flow);

            assertThat(flow.successCalled(), is(true));
            assertThat(authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE), nullValue());
        });
    }

    @Test
    void collectorClearsAuthNoteWhenVisitorIdMissingAndSucceeds() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var client = client(realm);
            var authSession = createAuthSession(session, realm, client);
            authSession.setAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE, VISITOR_ID);
            var flow = new RecordingAuthenticationFlowContext(realm, authSession);

            new DeviceFingerprintCollector().action(flow);

            assertThat(flow.successCalled(), is(true));
            assertThat(authSession.getAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE), nullValue());
        });
    }

    @Test
    void capsStoredDevicesAtConfiguredLimit() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);
            realm.setAttribute(KnownDeviceContext.MAX_STORED_DEVICES_CONFIG, "3");

            long now = Time.currentTime();
            setKnownDevices(user,
                    visitorId(0) + ":" + now,
                    visitorId(1) + ":" + now,
                    visitorId(2) + ":" + now);

            String fourth = visitorId(3);
            bindVisitorId(session, realm, client, fourth);
            UserContexts.getContext(session, KnownDeviceContext.class).onSuccessfulLogin(realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertThat(stored.size(), is(3));
            assertThat(stored, not(hasItem(startsWith(visitorId(0)))));
            assertThat(stored.stream().anyMatch(value -> value.startsWith(fourth)), is(true));
        });
    }

    @Test
    void capsStoredDevicesAtDefaultWhenUnset() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);

            long now = Time.currentTime();
            int limit = KnownDeviceContext.DEFAULT_MAX_STORED_DEVICES;
            List<String> seeded = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                seeded.add(visitorId(i) + ":" + now);
            }
            setKnownDevices(user, seeded.toArray(String[]::new));

            String next = visitorId(limit);
            bindVisitorId(session, realm, client, next);
            UserContexts.getContext(session, KnownDeviceContext.class).onSuccessfulLogin(realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertThat(stored.size(), is(limit));
            assertThat(stored, not(hasItem(startsWith(visitorId(0)))));
            assertThat(stored.stream().anyMatch(value -> value.startsWith(next)), is(true));
        });
    }

    @Test
    void refreshesTimestampForKnownDevice() {
        runOnServer.run(session -> {
            var realm = realm(session);
            var user = user(session, realm);
            var client = client(realm);
            prepareEvaluator(session, realm, user);

            long stale = Time.currentTime() - Duration.ofDays(30).getSeconds();
            setKnownDevices(user, VISITOR_ID + ":" + stale);

            long loginTime = Time.currentTime();
            bindVisitorId(session, realm, client, VISITOR_ID);
            UserContexts.getContext(session, KnownDeviceContext.class).onSuccessfulLogin(realm, user);

            var stored = user.getAttributes().get(KnownDeviceContext.KNOWN_DEVICES_ATTR);
            assertThat(stored.size(), is(1));
            var parsed = KnownDeviceData.parseFromAttribute(stored.getFirst());
            assertNotNull(parsed);
            assertThat(parsed.visitorId(), is(VISITOR_ID));
            assertThat(parsed.lastSeenEpochSeconds(), greaterThanOrEqualTo(loginTime));
            assertThat(parsed.lastSeenEpochSeconds() > stale, is(true));
        });
    }

    private static String visitorId(int index) {
        return String.format("%032x", index);
    }

    private static RealmModel realm(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("adaptive");
        assertNotNull(realm);
        return realm;
    }

    private static UserModel user(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, "user");
        assertNotNull(user);
        return user;
    }

    private static ClientModel client(RealmModel realm) {
        ClientModel client = realm.getClientByClientId("account");
        assertNotNull(client);
        return client;
    }

    private static void prepareEvaluator(KeycloakSession session, RealmModel realm, UserModel user) {
        user.removeAttribute(KnownDeviceContext.KNOWN_DEVICES_ATTR);
        realm.setAttribute(KnownDeviceContext.TTL_DAYS_CONFIG, "90");
        realm.removeAttribute(KnownDeviceContext.MAX_STORED_DEVICES_CONFIG);
        EvaluatorUtils.setEvaluatorEnabled(realm, KnownDeviceRiskEvaluator.class, true);
    }

    private static void setKnownDevices(UserModel user, String... attributeValues) {
        user.setAttribute(KnownDeviceContext.KNOWN_DEVICES_ATTR, List.of(attributeValues));
    }

    private static AuthenticationSessionModel createAuthSession(
            KeycloakSession session, RealmModel realm, ClientModel client) {
        RootAuthenticationSessionModel rootSession =
                session.authenticationSessions().createRootAuthenticationSession(realm);
        return rootSession.createAuthenticationSession(client);
    }

    private static void bindVisitorId(
            KeycloakSession session, RealmModel realm, ClientModel client, String visitorId) {
        AuthenticationSessionModel authSession = createAuthSession(session, realm, client);
        if (visitorId != null) {
            authSession.setAuthNote(KnownDeviceConstants.VISITOR_ID_AUTH_NOTE, visitorId);
        }
        session.getContext().setRealm(realm);
        session.getContext().setClient(client);
        session.getContext().setAuthenticationSession(authSession);
    }

    private static void fireLoginEvent(KeycloakSession session, RealmModel realm, UserModel user) {
        Event event = new Event();
        event.setType(EventType.LOGIN);
        event.setRealmId(realm.getId());
        event.setUserId(user.getId());

        EventListenerProvider listener = session.getProvider(
                EventListenerProvider.class, LoginEventsEventListenerFactory.PROVIDER_ID);
        assertNotNull(listener, "login-events-adaptive-authn listener should be registered");
        listener.onEvent(event);
    }

    /**
     * Records success/challenge against a real realm + auth session (no RealmModel stub).
     * Unused AuthenticationFlowContext methods are no-ops / null.
     */
    static final class RecordingAuthenticationFlowContext implements AuthenticationFlowContext {
        private final RealmModel realm;
        private final AuthenticationSessionModel authenticationSession;
        private final MultivaluedMap<String, String> formParameters = new MultivaluedHashMap<>();
        private boolean successCalled;
        private boolean challengeCalled;

        RecordingAuthenticationFlowContext(RealmModel realm, AuthenticationSessionModel authenticationSession) {
            this.realm = realm;
            this.authenticationSession = authenticationSession;
        }

        MultivaluedMap<String, String> formParameters() {
            return formParameters;
        }

        boolean successCalled() {
            return successCalled;
        }

        boolean challengeCalled() {
            return challengeCalled;
        }

        @Override
        public RealmModel getRealm() {
            return realm;
        }

        @Override
        public AuthenticationSessionModel getAuthenticationSession() {
            return authenticationSession;
        }

        @Override
        public HttpRequest getHttpRequest() {
            return new HttpRequest() {
                @Override
                public String getHttpMethod() {
                    return "POST";
                }

                @Override
                public MultivaluedMap<String, String> getDecodedFormParameters() {
                    return formParameters;
                }

                @Override
                public MultivaluedMap<String, org.keycloak.http.FormPartValue> getMultiPartFormParameters() {
                    return new MultivaluedHashMap<>();
                }

                @Override
                public jakarta.ws.rs.core.HttpHeaders getHttpHeaders() {
                    return null;
                }

                @Override
                public java.security.cert.X509Certificate[] getClientCertificateChain() {
                    return new java.security.cert.X509Certificate[0];
                }

                @Override
                public UriInfo getUri() {
                    return null;
                }

                @Override
                public boolean isProxyTrusted() {
                    return true;
                }
            };
        }

        @Override
        public void success() {
            successCalled = true;
        }

        @Override
        public void success(String credentialType) {
            successCalled = true;
        }

        @Override
        public void challenge(Response response) {
            challengeCalled = true;
        }

        @Override
        public LoginFormsProvider form() {
            throw new UnsupportedOperationException("not used by collector skip/action paths");
        }

        @Override
        public UserModel getUser() {
            return null;
        }

        @Override
        public void setUser(UserModel user) {
        }

        @Override
        public List<AuthenticationSelectionOption> getAuthenticationSelections() {
            return List.of();
        }

        @Override
        public void setAuthenticationSelections(List<AuthenticationSelectionOption> selections) {
        }

        @Override
        public void clearUser() {
        }

        @Override
        public void attachUserSession(UserSessionModel userSession) {
        }

        @Override
        public String getFlowPath() {
            return null;
        }

        @Override
        public URI getActionUrl(String code) {
            return null;
        }

        @Override
        public URI getActionTokenUrl(String token) {
            return null;
        }

        @Override
        public URI getRefreshExecutionUrl() {
            return null;
        }

        @Override
        public URI getRefreshUrl(boolean authSessionIdParam) {
            return null;
        }

        @Override
        public void cancelLogin() {
        }

        @Override
        public void resetFlow() {
        }

        @Override
        public void resetFlow(Runnable afterReset) {
        }

        @Override
        public void fork() {
        }

        @Override
        public void forkWithSuccessMessage(FormMessage message) {
        }

        @Override
        public void forkWithErrorMessage(FormMessage message) {
        }

        @Override
        public EventBuilder getEvent() {
            return null;
        }

        @Override
        public EventBuilder newEvent() {
            return null;
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return null;
        }

        @Override
        public AuthenticationFlowModel getTopLevelFlow() {
            return null;
        }

        @Override
        public ClientConnection getConnection() {
            return null;
        }

        @Override
        public UriInfo getUriInfo() {
            return null;
        }

        @Override
        public KeycloakSession getSession() {
            return null;
        }

        @Override
        public BruteForceProtector getProtector() {
            return null;
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfig() {
            return null;
        }

        @Override
        public FormMessage getForwardedErrorMessage() {
            return null;
        }

        @Override
        public FormMessage getForwardedSuccessMessage() {
            return null;
        }

        @Override
        public FormMessage getForwardedInfoMessage() {
            return null;
        }

        @Override
        public void setForwardedInfoMessage(String message, Object... parameters) {
        }

        @Override
        public String generateAccessCode() {
            return null;
        }

        @Override
        public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory) {
            return null;
        }

        @Override
        public void failure(AuthenticationFlowError error) {
        }

        @Override
        public void failure(AuthenticationFlowError error, Response response) {
        }

        @Override
        public void failure(AuthenticationFlowError error, Response response, String eventDetails, String userErrorMessage) {
        }

        @Override
        public void forceChallenge(Response response) {
            challenge(response);
        }

        @Override
        public void failureChallenge(AuthenticationFlowError error, Response response) {
        }

        @Override
        public void attempted() {
        }

        @Override
        public FlowStatus getStatus() {
            return successCalled ? FlowStatus.SUCCESS : null;
        }

        @Override
        public AuthenticationFlowError getError() {
            return null;
        }

        @Override
        public String getEventDetails() {
            return null;
        }

        @Override
        public String getUserErrorMessage() {
            return null;
        }
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.log().categoryLevel("io.github.mabartos", "debug");
            return builder.dependency("io.github.mabartos", "keycloak-adaptive-authn")
                    .dependency("io.github.mabartos", "keycloak-adaptive-ext-fingerprintjs")
                    .option("features", "declarative-ui");
        }
    }
}
