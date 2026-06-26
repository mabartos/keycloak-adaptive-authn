package io.github.mabartos.audit.admin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.AdminRoot;

import java.util.Map;

/**
 * Emits optional Keycloak admin events for adaptive configuration changes on update.
 */
public final class AdaptiveAdminAudit {

    private static final Logger logger = Logger.getLogger(AdaptiveAdminAudit.class);

    public static final String RESOURCE_TYPE = "ADAPTIVE_RISK_CONFIG";

    private static final ClientConnection UNKNOWN_CONNECTION = new ClientConnection() {
        @Override
        public String getRemoteAddr() {
            return "unknown";
        }

        @Override
        public String getRemoteHost() {
            return "unknown";
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalAddr() {
            return "unknown";
        }

        @Override
        public int getLocalPort() {
            return 0;
        }
    };

    private AdaptiveAdminAudit() {
    }

    /**
     * Records only realm policy settings that changed on update (one detail per key, value {@code old > new}).
     */
    public static void recordRiskPoliciesUpdate(
            @Nullable KeycloakSession session,
            @Nonnull RealmModel realm,
            @Nonnull Map<String, String> beforeSettings,
            @Nonnull Map<String, String> afterSettings
    ) {
        recordRiskPoliciesUpdate(session, realm, beforeSettings, afterSettings, null);
    }

    /**
     * @param adminAuthOverride when non-null, used instead of resolving auth from the HTTP request (integration tests).
     */
    public static void recordRiskPoliciesUpdate(
            @Nullable KeycloakSession session,
            @Nonnull RealmModel realm,
            @Nonnull Map<String, String> beforeSettings,
            @Nonnull Map<String, String> afterSettings,
            @Nullable AdminAuth adminAuthOverride
    ) {
        recordConfigUpdate(
                session,
                realm,
                new String[]{"realms", realm.getName(), "authentication", "risk-based-policies"},
                beforeSettings,
                afterSettings,
                "realm policies",
                adminAuthOverride
        );
    }

    private static void recordConfigUpdate(
            @Nullable KeycloakSession session,
            @Nonnull RealmModel realm,
            @Nonnull String[] resourcePath,
            @Nonnull Map<String, String> beforeSettings,
            @Nonnull Map<String, String> afterSettings,
            @Nonnull String logContext,
            @Nullable AdminAuth adminAuthOverride
    ) {
        if (session == null || !AdaptiveAdminAuditConfig.shouldRecordAdaptiveAdminConfigAudit(realm, beforeSettings, afterSettings)) {
            return;
        }

        var changes = RiskPoliciesSettingsSnapshot.diff(beforeSettings, afterSettings);
        if (changes.isEmpty()) {
            logger.debugf("Skipping adaptive admin audit for %s in realm %s: no setting changes",
                    logContext, realm.getName());
            return;
        }

        AdminAuth auth = adminAuthOverride != null ? adminAuthOverride : resolveAdminAuth(session);
        if (auth == null) {
            logger.debugf("Skipping adaptive admin audit for realm %s: no admin authentication context", realm.getName());
            return;
        }

        try {
            var builder = new AdminEventBuilder(realm, auth, session, resolveClientConnection(session))
                    .resource(RESOURCE_TYPE)
                    .operation(OperationType.UPDATE)
                    .resourcePath(resourcePath);

            buildPolicyChangeDetails(changes).forEach(builder::detail);
            builder.success();
            logger.debugf("Recorded adaptive admin audit for %s in realm %s (%d changed settings)",
                    logContext, realm.getName(), changes.size());
        } catch (RuntimeException e) {
            logger.warnf(e, "Failed to record adaptive admin audit for %s in realm %s", logContext, realm.getName());
        }
    }

    /**
     * Detail key = config attribute name, value = {@code old > new}.
     */
    static Map<String, String> buildPolicyChangeDetails(Map<String, String> changes) {
        var details = new java.util.LinkedHashMap<String, String>();
        changes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> details.put(
                        entry.getKey(),
                        RiskPoliciesSettingsSnapshot.sanitizeDetailValue(entry.getValue())
                ));
        return details;
    }

    @Nullable
    static AdminAuth resolveAdminAuth(KeycloakSession session) {
        try {
            return AdminRoot.authenticateRealmAdminRequest(session);
        } catch (RuntimeException e) {
            logger.tracef("Admin authentication unavailable for adaptive admin audit: %s", e.getMessage());
            return null;
        }
    }

    static ClientConnection resolveClientConnection(KeycloakSession session) {
        try {
            var connection = session.getContext().getConnection();
            if (connection != null) {
                return connection;
            }
        } catch (RuntimeException e) {
            logger.tracef("Client connection unavailable for adaptive admin audit: %s", e.getMessage());
        }
        return UNKNOWN_CONNECTION;
    }
}
