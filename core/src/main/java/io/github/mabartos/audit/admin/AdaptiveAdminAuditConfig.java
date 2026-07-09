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
     * @return {@code true} when realm admin events are enabled and the Risk policy change audit toggle is on.
     */
    public static boolean isAdminAuditEnabled(RealmModel realm) {
        return realm != null
                && realm.isAdminEventsEnabled()
                && parseToggle(realm.getAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
    }

    /**
     * @return {@code true} when a diff admin event may be emitted for this update.
     * Requires realm admin events and the tab toggle on after save, or on in the before snapshot
     * (e.g. disabling audit in the same save).
     */
    public static boolean shouldRecordAdaptiveAdminConfigAudit(
            RealmModel realm,
            Map<String, String> beforeSettings,
            Map<String, String> afterSettings
    ) {
        if (realm == null || !realm.isAdminEventsEnabled() || beforeSettings == null || afterSettings == null) {
            return false;
        }
        return isAdminAuditEnabled(realm)
                || parseToggle(beforeSettings.get(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
    }

    private static boolean parseToggle(String value) {
        return Optional.ofNullable(value)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }
}
