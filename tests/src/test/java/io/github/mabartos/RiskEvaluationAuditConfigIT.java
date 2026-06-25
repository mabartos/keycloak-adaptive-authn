package io.github.mabartos;

import io.github.mabartos.engine.core.RiskEvaluationAuditConfig;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mabartos.engine.core.RiskEvaluationAuditConfig.AUDIT_EVENT_TYPE_NAME;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.AUDIT_EVENTS_ENABLED_CONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = RiskEvaluationAuditConfigIT.Config.class)
class RiskEvaluationAuditConfigIT {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void isAuditEnabled_falseWhenAdaptiveSwitchOff() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setEventsEnabled(true);
                realm.setEnabledEventTypes(Set.of(AUDIT_EVENT_TYPE_NAME));
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, "false");
                assertThat(RiskEvaluationAuditConfig.isAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAuditEnabled_falseWhenRealmEventsDisabled() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setEventsEnabled(false);
                realm.setEnabledEventTypes(Set.of(AUDIT_EVENT_TYPE_NAME));
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, "true");
                assertThat(RiskEvaluationAuditConfig.isAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAuditEnabled_falseWhenSavedEventTypesEmpty() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setEventsEnabled(true);
                realm.setEnabledEventTypes(Set.of());
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, "true");
                assertThat(RiskEvaluationAuditConfig.isAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAuditEnabled_falseWhenAuditTypeNotListed() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setEventsEnabled(true);
                realm.setEnabledEventTypes(Set.of("LOGIN"));
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, "true");
                assertThat(RiskEvaluationAuditConfig.isAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAuditEnabled_trueWhenFullyConfigured() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setEventsEnabled(true);
                realm.setEnabledEventTypes(Set.of("LOGIN", AUDIT_EVENT_TYPE_NAME));
                realm.setAttribute(AUDIT_EVENTS_ENABLED_CONFIG, "true");
                assertThat(RiskEvaluationAuditConfig.isAuditEnabled(realm), is(true));
            } finally {
                snapshot.restore(realm);
            }
        });
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
