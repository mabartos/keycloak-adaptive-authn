package io.github.mabartos;

import io.github.mabartos.audit.admin.AdaptiveAdminAudit;
import org.junit.jupiter.api.Test;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.util.Map;
import java.util.Optional;

import static io.github.mabartos.audit.admin.AdaptiveAdminAudit.RESOURCE_TYPE;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;
import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.RISK_BASED_AUTHN_ENABLED_CONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = AdaptiveAdminAuditIT.Config.class)
class AdaptiveAdminAuditIT {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void recordRiskPoliciesUpdate_persistsAdminEventWhenEnabledAndChanged() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAdminAudit(realm, true);
                clearAdaptiveAdminEvents(session, realm);

                var before = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "true"
                );
                var after = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "false"
                );

                AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                        session, realm, before, after, adminAuth(session, realm));

                var event = latestAdaptiveAdminEvent(session, realm).orElseThrow();
                assertThat(event.getResourceTypeAsString(), is(RESOURCE_TYPE));
                assertThat(event.getOperationType(), is(OperationType.UPDATE));
                assertThat(event.getDetails().get(RISK_BASED_AUTHN_ENABLED_CONFIG), is("true > false"));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordRiskPoliciesUpdate_skipsWhenNothingChanged() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAdminAudit(realm, true);
                clearAdaptiveAdminEvents(session, realm);

                var unchanged = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "true"
                );

                AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                        session, realm, unchanged, unchanged, adminAuth(session, realm));

                assertThat(latestAdaptiveAdminEvent(session, realm), is(Optional.empty()));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordRiskPoliciesUpdate_skipsWhenBothToggleSnapshotsOff() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAdminAudit(realm, false);
                clearAdaptiveAdminEvents(session, realm);

                var before = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "true"
                );
                var after = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "false"
                );

                AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                        session, realm, before, after, adminAuth(session, realm));

                assertThat(latestAdaptiveAdminEvent(session, realm), is(Optional.empty()));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordRiskPoliciesUpdate_logsToggleEnableInSameSave() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                clearAdaptiveAdminEvents(session, realm);

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                        session, realm, before, after, adminAuth(session, realm));

                var event = latestAdaptiveAdminEvent(session, realm).orElseThrow();
                assertThat(event.getDetails().get(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG), is("false > true"));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void recordRiskPoliciesUpdate_logsToggleDisableInSameSave() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = RealmSnapshot.capture(realm);
            try {
                configureAdminAudit(realm, true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
                clearAdaptiveAdminEvents(session, realm);

                var before = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "true"
                );
                var after = Map.of(
                        ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false",
                        RISK_BASED_AUTHN_ENABLED_CONFIG, "false"
                );

                AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                        session, realm, before, after, adminAuth(session, realm));

                var event = latestAdaptiveAdminEvent(session, realm).orElseThrow();
                assertThat(event.getDetails().get(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG), is("true > false"));
                assertThat(event.getDetails().get(RISK_BASED_AUTHN_ENABLED_CONFIG), is("true > false"));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    private static void configureAdminAudit(RealmModel realm, boolean toggleOn) {
        realm.setAdminEventsEnabled(true);
        realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, toggleOn ? "true" : "false");
    }

    private static void clearAdaptiveAdminEvents(KeycloakSession session, RealmModel realm) {
        var store = session.getProvider(EventStoreProvider.class);
        if (store != null) {
            store.clearAdmin(realm);
        }
    }

    private static Optional<AdminEvent> latestAdaptiveAdminEvent(KeycloakSession session, RealmModel realm) {
        var store = session.getProvider(EventStoreProvider.class);
        if (store == null) {
            return Optional.empty();
        }
        return store.createAdminQuery()
                .realm(realm.getId())
                .operation(OperationType.UPDATE)
                .orderByDescTime()
                .maxResults(10)
                .getResultStream()
                .filter(event -> RESOURCE_TYPE.equals(event.getResourceTypeAsString()))
                .findFirst();
    }

    private static AdminAuth adminAuth(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, "admin");
        ClientModel client = realm.getClientByClientId("admin-cli");
        return new AdminAuth(realm, new AccessToken(), user, client);
    }

    private static final class RealmSnapshot {
        private final boolean adminEventsEnabled;
        private final String configAuditToggle;

        private RealmSnapshot(boolean adminEventsEnabled, String configAuditToggle) {
            this.adminEventsEnabled = adminEventsEnabled;
            this.configAuditToggle = configAuditToggle;
        }

        static RealmSnapshot capture(RealmModel realm) {
            return new RealmSnapshot(
                    realm.isAdminEventsEnabled(),
                    realm.getAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
        }

        void restore(RealmModel realm) {
            realm.setAdminEventsEnabled(adminEventsEnabled);
            if (configAuditToggle != null) {
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, configAuditToggle);
            } else {
                realm.removeAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG);
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
