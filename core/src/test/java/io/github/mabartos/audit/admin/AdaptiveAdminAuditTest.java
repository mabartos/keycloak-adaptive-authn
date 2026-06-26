package io.github.mabartos.audit.admin;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Map;
import java.util.Optional;

import static io.github.mabartos.ui.RiskBasedPoliciesUiTab.ADMIN_CONFIG_AUDIT_ENABLED_CONFIG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdaptiveAdminAuditTest {

    @Test
    void recordRiskPoliciesUpdate_skipsWhenSessionNull() {
        assertDoesNotThrow(() -> AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                null,
                adminEventsRealm(true, "true"),
                Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true"),
                Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false")
        ));
    }

    @Test
    void recordRiskPoliciesUpdate_skipsWhenAdminEventsDisabled() {
        assertDoesNotThrow(() -> AdaptiveAdminAudit.recordRiskPoliciesUpdate(
                mockSession(),
                adminEventsRealm(false, "true"),
                Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "true"),
                Map.of(ADMIN_CONFIG_AUDIT_ENABLED_CONFIG, "false")
        ));
    }

    private static KeycloakSession mockSession() {
        return (KeycloakSession) java.lang.reflect.Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] {KeycloakSession.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static RealmModel adminEventsRealm(boolean adminEventsEnabled, String configAuditToggle) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isAdminEventsEnabled" -> adminEventsEnabled;
                    case "getAttribute" -> ADMIN_CONFIG_AUDIT_ENABLED_CONFIG.equals(args[0])
                            ? configAuditToggle
                            : null;
                    case "getName" -> "adaptive";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == Optional.class) {
            return Optional.empty();
        }
        return null;
    }
}
