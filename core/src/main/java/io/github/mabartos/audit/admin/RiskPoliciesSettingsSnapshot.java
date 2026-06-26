package io.github.mabartos.audit.admin;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Collects and formats the effective realm-wide adaptive policy settings shown on the
 * Risk-based policies admin tab.
 */
public final class RiskPoliciesSettingsSnapshot {

    static final String DIFF_VALUE_SEPARATOR = " > ";

    private RiskPoliciesSettingsSnapshot() {
    }

    /**
     * Keys whose effective value changed between two snapshots (values formatted as {@code old > new}).
     */
    public static Map<String, String> diff(Map<String, String> before, Map<String, String> after) {
        var changes = new LinkedHashMap<String, String>();
        if (after == null || after.isEmpty()) {
            return changes;
        }
        var previous = before != null ? before : Map.<String, String>of();
        for (var entry : after.entrySet()) {
            var key = entry.getKey();
            var oldValue = previous.getOrDefault(key, "");
            var newValue = entry.getValue() != null ? entry.getValue() : "";
            if (!Objects.equals(oldValue, newValue)) {
                changes.put(key, formatDiffValue(oldValue, newValue));
            }
        }
        return changes;
    }

    /**
     * Reads the effective value for each configured tab property from realm attributes,
     * falling back to declared defaults when no attribute is stored.
     */
    public static Map<String, String> collect(RealmModel realm, List<ProviderConfigProperty> configProperties) {
        var snapshot = new LinkedHashMap<String, String>();
        if (realm == null || configProperties == null) {
            return snapshot;
        }
        for (ProviderConfigProperty property : configProperties) {
            var key = property.getName();
            if (StringUtil.isBlank(key)) {
                continue;
            }
            snapshot.put(key, resolveEffectiveValue(realm, property));
        }
        return snapshot;
    }

    static String resolveEffectiveValue(RealmModel realm, ProviderConfigProperty property) {
        var stored = realm.getAttribute(property.getName());
        if (StringUtil.isNotBlank(stored)) {
            return stored;
        }
        if (property.getDefaultValue() != null) {
            return property.getDefaultValue().toString();
        }
        return "";
    }

    static String formatDiffValue(String oldValue, String newValue) {
        return sanitizeDetailValue(oldValue) + DIFF_VALUE_SEPARATOR + sanitizeDetailValue(newValue);
    }

    static String sanitizeDetailValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
