package io.github.mabartos.audit.admin;

import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RiskPoliciesSettingsSnapshotTest {

    @Test
    void diff_returnsOnlyChangedKeysWithOldNewValues() {
        var before = Map.of(
                "adaptive-engine-enabled", "true",
                "adaptive-engine-timeout", "2500"
        );
        var after = Map.of(
                "adaptive-engine-enabled", "false",
                "adaptive-engine-timeout", "2500"
        );

        var changes = RiskPoliciesSettingsSnapshot.diff(before, after);

        assertThat(changes.size(), is(1));
        assertThat(changes.get("adaptive-engine-enabled"), is("true > false"));
    }

    @Test
    void diff_emptyWhenNothingChanged() {
        var snapshot = Map.of("adaptive-engine-enabled", "true");
        assertThat(RiskPoliciesSettingsSnapshot.diff(snapshot, snapshot).isEmpty(), is(true));
    }

    @Test
    void collect_usesRealmAttributeThenDefault() {
        var properties = List.of(
                property("adaptive-engine-enabled", "true"),
                property("adaptive-engine-timeout", "2500")
        );
        var realm = realmWithAttributes(Map.of("adaptive-engine-enabled", "false"));

        var snapshot = RiskPoliciesSettingsSnapshot.collect(realm, properties);

        assertThat(snapshot.get("adaptive-engine-enabled"), is("false"));
        assertThat(snapshot.get("adaptive-engine-timeout"), is("2500"));
    }

    private static ProviderConfigProperty property(String name, Object defaultValue) {
        var property = new ProviderConfigProperty();
        property.setName(name);
        property.setDefaultValue(defaultValue);
        return property;
    }

    private static RealmModel realmWithAttributes(Map<String, String> attributes) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "getId" -> "realm-id";
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
