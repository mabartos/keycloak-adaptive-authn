package io.github.mabartos;

import io.github.mabartos.audit.admin.AdaptiveAdminAuditConfig;
import io.github.mabartos.support.AdminConfigAuditRealmSnapshot;
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

import java.util.Map;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = AdaptiveAdminAuditConfigIT.Config.class)
class AdaptiveAdminAuditConfigIT {

    @InjectRealm(config = AdaptiveRealmConfig.class, ref = "adaptive", lifecycle = LifeCycle.CLASS)
    ManagedRealm adaptiveRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void isAdminAuditEnabled_falseWhenToggleOff() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
                assertThat(AdaptiveAdminAuditConfig.isAdminAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAdminAuditEnabled_falseWhenAdminEventsDisabled() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(false);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                assertThat(AdaptiveAdminAuditConfig.isAdminAuditEnabled(realm), is(false));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void isAdminAuditEnabled_trueWhenFullyConfigured() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                assertThat(AdaptiveAdminAuditConfig.isAdminAuditEnabled(realm), is(true));
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void shouldRecord_falseWhenBothToggleSnapshotsOff() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

                assertThat(
                        AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, before, after),
                        is(false)
                );
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void shouldRecord_trueWhenToggleEnabledInSameSave() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                assertThat(
                        AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, before, after),
                        is(true)
                );
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void shouldRecord_trueWhenToggleDisabledInSameSave() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

                assertThat(
                        AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, before, after),
                        is(true)
                );
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void shouldRecord_trueWhenToggleWasAlreadyOn() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(true);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                assertThat(
                        AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, before, after),
                        is(true)
                );
            } finally {
                snapshot.restore(realm);
            }
        });
    }

    @Test
    void shouldRecord_falseWhenAdminEventsDisabled() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("adaptive");
            var snapshot = AdminConfigAuditRealmSnapshot.capture(realm);
            try {
                realm.setAdminEventsEnabled(false);
                realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
                var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

                assertThat(
                        AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, before, after),
                        is(false)
                );
            } finally {
                snapshot.restore(realm);
            }
        });
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
