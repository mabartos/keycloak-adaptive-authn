package io.github.mabartos;

import io.github.mabartos.engine.core.RiskEvaluationAuditPublisher;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.ResultRisk;
import org.junit.jupiter.api.Test;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mabartos.engine.core.RiskEvaluationAuditConfig.AUDIT_EVENT_TYPE_NAME;
import static io.github.mabartos.engine.core.RiskEvaluationAuditPublisher.AUTH_NOTE_BEFORE_AUTHN_EVALUATORS;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.AUDIT_EVENTS_ENABLED_CONFIG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@KeycloakIntegrationTest(config = RiskEvaluationAuditPublisherRecordFromStoredTest.Config.class)
class RiskEvaluationAuditPublisherRecordFromStoredTest {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void recordLoginEvaluationFromStored_skipsWhenAuditDisabled() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            UserModel user = requireUser(session, realm);
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAudit(realm, false);
                withAuthSession(session, realm, () -> {
                    storeLoginRisks(session, 0.55, null, null, null);
                    var publisher = RiskEvaluationAuditPublisher.forSession(session);
                    publisher.recordLoginEvaluationFromStored(realm, user);
                    assertThat(pendingEvents(publisher), is(empty()));
                });
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordLoginEvaluationFromStored_skipsWhenStoredRisksInvalid() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            UserModel user = requireUser(session, realm);
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAudit(realm, true);
                withAuthSession(session, realm, () -> {
                    var publisher = RiskEvaluationAuditPublisher.forSession(session);
                    publisher.recordLoginEvaluationFromStored(realm, user);
                    assertThat(pendingEvents(publisher), is(empty()));
                });
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordLoginEvaluationFromStored_queuesLoginSnapshotFromStoredProvider() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            UserModel user = requireUser(session, realm);
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAudit(realm, true);
                withAuthSession(session, realm, () -> {
                    storeLoginRisks(session, 0.55, 0.20, 0.15, "BrowserRiskEvaluator=NONE");
                    var publisher = RiskEvaluationAuditPublisher.forSession(session);
                    publisher.recordLoginEvaluationFromStored(realm, user);

                    var pending = pendingEvents(publisher);
                    assertThat(pending.size(), is(1));
                    var login = pending.getFirst();
                    assertThat(login.getClass().getSimpleName(), is("LoginAuditEvent"));
                    assertThat(invokeOptionalString(login, "userKnownScore"), is("0.5500"));
                    assertThat(invokeOptionalString(login, "userKnownLevel"), is("MEDIUM"));
                    assertThat(invokeOptionalString(login, "beforeAuthnScore"), is("0.2000"));
                    assertThat(invokeOptionalString(login, "beforeAuthnLevel"), is("LOW"));
                    assertThat(invokeOptionalString(login, "overallScore"), is("0.1500"));
                    assertThat(invokeOptionalString(login, "overallLevel"), is("LOW"));
                    assertThat(invokeOptionalString(login, "beforeAuthnEvaluators"), is("BrowserRiskEvaluator=NONE"));
                    assertThat(invokeOptional(login, "userKnownEvaluators", Optional.class), is(Optional.empty()));
                });
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordLoginEvaluationFromStored_queuesWhenOnlyOverallRiskValid() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            UserModel user = requireUser(session, realm);
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAudit(realm, true);
                withAuthSession(session, realm, () -> {
                    storeLoginRisks(session, null, null, 0.72, null);
                    var publisher = RiskEvaluationAuditPublisher.forSession(session);
                    publisher.recordLoginEvaluationFromStored(realm, user);

                    assertThat(pendingEvents(publisher).size(), is(1));
                    var login = pendingEvents(publisher).getFirst();
                    assertThat(invokeOptionalString(login, "overallScore"), is("0.7200"));
                    assertThat(invokeOptionalString(login, "overallLevel"), is("MEDIUM"));
                    assertThat(invokeOptional(login, "userKnownScore", Optional.class), is(Optional.empty()));
                });
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    private static UserModel requireUser(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, "user");
        assertThat("Expected test user in adaptive realm", user != null, is(true));
        return user;
    }

    private static void configureAudit(RealmModel realm, boolean auditEnabled) {
        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Set.of(AUDIT_EVENT_TYPE_NAME));
        realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, Boolean.toString(auditEnabled));
    }

    private static void withAuthSession(KeycloakSession session, RealmModel realm, Runnable action) {
        ClientModel client = realm.getClientByClientId("account");
        assertThat("Expected account client in adaptive realm", client != null, is(true));
        RootAuthenticationSessionModel root = session.authenticationSessions().createRootAuthenticationSession(realm);
        try {
            AuthenticationSessionModel authSession = root.createAuthenticationSession(client);
            session.getContext().setRealm(realm);
            session.getContext().setAuthenticationSession(authSession);
            action.run();
        } finally {
            session.authenticationSessions().removeRootAuthenticationSession(realm, root);
        }
    }

    private static void storeLoginRisks(
            KeycloakSession session,
            Double userKnownScore,
            Double beforeAuthnScore,
            Double overallScore,
            String beforeAuthnEvaluators) {
        StoredRiskProvider stored = session.getProvider(StoredRiskProvider.class);
        if (beforeAuthnScore != null) {
            stored.storeRisk(ResultRisk.of(beforeAuthnScore), RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        }
        if (userKnownScore != null) {
            stored.storeRisk(ResultRisk.of(userKnownScore), RiskEvaluator.EvaluationPhase.USER_KNOWN);
        }
        if (overallScore != null) {
            stored.storeOverallRisk(ResultRisk.of(overallScore));
        }
        if (beforeAuthnEvaluators != null) {
            stored.storeAdditionalData(AUTH_NOTE_BEFORE_AUTHN_EVALUATORS, beforeAuthnEvaluators);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> pendingEvents(RiskEvaluationAuditPublisher publisher) {
        try {
            Field field = RiskEvaluationAuditPublisher.class.getDeclaredField("pending");
            field.setAccessible(true);
            return (List<Object>) field.get(publisher);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to read pending audit events", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String invokeOptionalString(Object target, String methodName) {
        return ((Optional<String>) invokeOptional(target, methodName, Optional.class)).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeOptional(Object target, String methodName, Class<T> returnType) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (T) method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to invoke " + methodName + " on " + target.getClass().getName(), e);
        }
    }

    private static final class RealmSnapshot {
        private final boolean eventsEnabled;
        private final Set<String> enabledEventTypes;
        private final String auditSwitchAttribute;

        private RealmSnapshot(boolean eventsEnabled, Set<String> enabledEventTypes, String auditSwitchAttribute) {
            this.eventsEnabled = eventsEnabled;
            this.enabledEventTypes = enabledEventTypes;
            this.auditSwitchAttribute = auditSwitchAttribute;
        }

        static RealmSnapshot capture(RealmModel realm) {
            var types = realm.getEnabledEventTypesStream().collect(Collectors.toSet());
            return new RealmSnapshot(
                    realm.isEventsEnabled(),
                    types,
                    realm.getAttribute(AUDIT_EVENTS_ENABLED_CONFIG));
        }

        void restore(RealmModel realm) {
            realm.setEventsEnabled(eventsEnabled);
            realm.setEnabledEventTypes(enabledEventTypes);
            if (auditSwitchAttribute != null) {
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, auditSwitchAttribute);
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
