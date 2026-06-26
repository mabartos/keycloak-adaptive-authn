package io.github.mabartos.audit.admin;

import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;

import java.util.Map;
import java.util.Optional;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AdaptiveAdminAuditConfigTest {

    @Test
    void shouldRecord_falseWhenBothToggleSnapshotsOff() {
        var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
        var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

        assertThat(
                AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm(true), before, after),
                is(false)
        );
    }

    @Test
    void shouldRecord_trueWhenToggleEnabledInSameSave() {
        var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");
        var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

        assertThat(
                AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm(true), before, after),
                is(true)
        );
    }

    @Test
    void shouldRecord_trueWhenToggleDisabledInSameSave() {
        var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
        var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false");

        assertThat(
                AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm(true), before, after),
                is(true)
        );
    }

    @Test
    void shouldRecord_trueWhenToggleWasAlreadyOn() {
        var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
        var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

        assertThat(
                AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm(true), before, after),
                is(true)
        );
    }

    @Test
    void shouldRecord_falseWhenAdminEventsDisabled() {
        var before = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");
        var after = Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true");

        assertThat(
                AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm(false), before, after),
                is(false)
        );
    }

    private static RealmModel realm(boolean adminEventsEnabled) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isAdminEventsEnabled" -> adminEventsEnabled;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == Optional.class) {
            return Optional.empty();
        }
        return null;
    }
}
