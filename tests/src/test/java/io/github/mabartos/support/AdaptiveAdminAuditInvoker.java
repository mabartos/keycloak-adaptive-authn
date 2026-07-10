package io.github.mabartos.support;

import io.github.mabartos.audit.admin.AdaptiveAdminAudit;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminAuth;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Invokes package-private audit recording hooks from integration tests without exposing them in production API.
 */
public final class AdaptiveAdminAuditInvoker {

    private AdaptiveAdminAuditInvoker() {
    }

    public static void recordRiskPoliciesUpdate(
            KeycloakSession session,
            RealmModel realm,
            Map<String, String> beforeSettings,
            Map<String, String> afterSettings,
            AdminAuth adminAuth
    ) {
        try {
            Method method = AdaptiveAdminAudit.class.getDeclaredMethod(
                    "recordRiskPoliciesUpdate",
                    KeycloakSession.class,
                    RealmModel.class,
                    Map.class,
                    Map.class,
                    AdminAuth.class);
            method.setAccessible(true);
            method.invoke(null, session, realm, beforeSettings, afterSettings, adminAuth);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke AdaptiveAdminAudit for integration tests", e);
        }
    }
}
