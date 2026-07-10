package io.github.mabartos.support;

import org.keycloak.models.RealmModel;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;

/**
 * Restores realm admin-event and Risk policy change audit toggle after integration tests.
 */
public final class AdminConfigAuditRealmSnapshot {

    private final boolean adminEventsEnabled;
    private final String configAuditToggle;

    private AdminConfigAuditRealmSnapshot(boolean adminEventsEnabled, String configAuditToggle) {
        this.adminEventsEnabled = adminEventsEnabled;
        this.configAuditToggle = configAuditToggle;
    }

    public static AdminConfigAuditRealmSnapshot capture(RealmModel realm) {
        return new AdminConfigAuditRealmSnapshot(
                realm.isAdminEventsEnabled(),
                realm.getAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG));
    }

    public void restore(RealmModel realm) {
        realm.setAdminEventsEnabled(adminEventsEnabled);
        if (configAuditToggle != null) {
            realm.setAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, configAuditToggle);
        } else {
            realm.removeAttribute(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG);
        }
    }
}
