package io.github.mabartos.test;

import org.keycloak.models.RealmModel;

import java.util.Map;

/**
 * Minimal {@link RealmModel} test double backed by a realm-attribute map.
 */
public final class RealmModelTestSupport {

    private RealmModelTestSupport() {
    }

    public static RealmModel realmWithAttributes(Map<String, String> attributes) {
        return (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] {RealmModel.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length == 1) {
                        return attributes.get(args[0].toString());
                    }
                    return defaultValue(method.getReturnType());
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
        return null;
    }
}
