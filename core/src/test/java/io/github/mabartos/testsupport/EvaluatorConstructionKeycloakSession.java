package io.github.mabartos.testsupport;

import io.github.mabartos.context.location.KnownLocationContext;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.context.user.TypicalAccessTimeContext;
import io.github.mabartos.context.user.UserRoleContext;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.context.location.LocationContext;
import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Minimal {@link KeycloakSession} for constructing {@link io.github.mabartos.spi.evaluator.RiskEvaluator}
 * instances in unit tests (e.g. reading {@code evaluationPhases()}).
 */
public final class EvaluatorConstructionKeycloakSession {

    private static final List<Class<? extends UserContext<?>>> USER_CONTEXT_TYPES = List.of(
            UserRoleContext.class,
            LocationContext.class,
            KnownLocationContext.class,
            TypicalAccessTimeContext.class,
            DeviceRepresentationContext.class,
            IpAddressContext.class);

    private EvaluatorConstructionKeycloakSession() {
    }

    public static KeycloakSession create() {
        KeycloakSession[] holder = new KeycloakSession[1];
        KeycloakSessionFactory sessionFactory = sessionFactory(holder);
        holder[0] = (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] {KeycloakSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKeycloakSessionFactory" -> sessionFactory;
                    case "getProvider" -> provider(holder[0], (Class<?>) args[0], args.length > 1 ? (String) args[1] : null);
                    case "getContext" -> keycloakContext();
                    default -> defaultValue(method.getReturnType());
                });
        return holder[0];
    }

    private static KeycloakSessionFactory sessionFactory(KeycloakSession[] sessionHolder) {
        return (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] {KeycloakSessionFactory.class},
                (proxy, method, args) -> {
                    if ("getProviderFactoriesStream".equals(method.getName())
                            && args.length == 1
                            && args[0] == UserContext.class) {
                        return USER_CONTEXT_TYPES.stream()
                                .map(EvaluatorConstructionKeycloakSession::userContextFactory);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static UserContextFactory<?> userContextFactory(Class<? extends UserContext<?>> type) {
        return (UserContextFactory<?>) Proxy.newProxyInstance(
                UserContextFactory.class.getClassLoader(),
                new Class<?>[] {UserContextFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUserContextClass" -> type;
                    case "getId" -> "test-" + type.getSimpleName();
                    case "getPriority" -> 0;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object provider(KeycloakSession session, Class<?> type, String providerId) {
        if (EventStoreProvider.class.equals(type)) {
            return interfaceProxy(EventStoreProvider.class);
        }
        if (UserContext.class.equals(type) && providerId != null) {
            if (KcLoginEventsContextFactory.PROVIDER_ID.equals(providerId)
                    || LoginEventsContext.LOGIN_EVENTS.equals(providerId)) {
                return EvaluatorTestContexts.loginEvents();
            }
            return userContextByTestId(session, providerId);
        }
        if (type.isInterface()) {
            return interfaceProxy(type);
        }
        return null;
    }

    private static UserContext<?> userContextByTestId(KeycloakSession session, String providerId) {
        return switch (providerId) {
            case "test-UserRoleContext" -> EvaluatorTestContexts.userRole();
            case "test-LocationContext" -> EvaluatorTestContexts.location();
            case "test-KnownLocationContext" -> new KnownLocationContext(null);
            case "test-TypicalAccessTimeContext" -> EvaluatorTestContexts.typicalAccessTime(session);
            case "test-DeviceRepresentationContext" -> EvaluatorTestContexts.deviceRepresentation();
            case "test-IpAddressContext" -> EvaluatorTestContexts.ipAddress();
            default -> throw new IllegalStateException("Unexpected test user context id: " + providerId);
        };
    }

    private static KeycloakContext keycloakContext() {
        return (KeycloakContext) Proxy.newProxyInstance(
                KeycloakContext.class.getClassLoader(),
                new Class<?>[] {KeycloakContext.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object interfaceProxy(Class<?> type) {
        return Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
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
        if (returnType == Stream.class) {
            return Stream.empty();
        }
        return null;
    }
}
