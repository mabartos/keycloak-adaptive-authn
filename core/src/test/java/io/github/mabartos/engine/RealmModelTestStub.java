package io.github.mabartos.engine;

import org.keycloak.models.RealmModel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Minimal {@link RealmModel} stub for unit tests (events enabled flag and saved event types).
 */
final class RealmModelTestStub {

    private RealmModelTestStub() {
    }

    static RealmModel realm(boolean eventsEnabled, String... savedEventTypes) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModelTestStub.class.getClassLoader(),
                new Class[] {RealmModel.class},
                new RealmStubInvocationHandler(eventsEnabled, savedEventTypes));
    }

    private static final class RealmStubInvocationHandler implements InvocationHandler {
        private final boolean eventsEnabled;
        private final String[] savedEventTypes;

        private RealmStubInvocationHandler(boolean eventsEnabled, String[] savedEventTypes) {
            this.eventsEnabled = eventsEnabled;
            this.savedEventTypes = savedEventTypes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "isEventsEnabled" -> eventsEnabled;
                case "getEnabledEventTypesStream" -> Arrays.stream(savedEventTypes);
                case "getName" -> "test-realm";
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "RealmModelStub";
                default -> null;
            };
        }
    }
}
