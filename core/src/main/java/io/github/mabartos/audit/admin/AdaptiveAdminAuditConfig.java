package io.github.mabartos.audit.admin;

import org.keycloak.models.RealmModel;

import java.util.Map;
import java.util.Optional;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;

/**
 * Whether adaptive configuration changes are persisted as custom Keycloak admin event details.
 */
public final class AdaptiveAdminAuditConfig {

    private AdaptiveAdminAuditConfig() {
    }

    /**
     * @return {@code true} when the realm stores admin events
     */
    public static boolean isAdminAuditEnabled(RealmModel realm) {
        return realm != null && realm.isAdminEventsEnabled();
    }

    /**
     * @return {@code true} when a diff admin event may be emitted for this update.
     * Requires realm admin events and the tab toggle on in either snapshot (both off → no event).
     */
    public static boolean shouldRecordAdaptiveAdminConfigAudit(
            RealmModel realm,
            Map<String, String> beforeSettings,
            Map<String, String> afterSettings
    ) {
        if (!isAdminAuditEnabled(realm) || beforeSettings == null || afterSettings == null) {
            return false;
        }
        boolean beforeOn = parseToggle(beforeSettings.get(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
        boolean afterOn = parseToggle(afterSettings.get(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
        return beforeOn || afterOn;
    }

    private static boolean parseToggle(String value) {
        return Optional.ofNullable(value)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }
}
