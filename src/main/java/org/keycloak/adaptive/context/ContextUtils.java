package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.adaptive.spi.condition.UserContextCondition;
import org.keycloak.models.KeycloakSession;

import java.util.Comparator;
import java.util.List;

public class ContextUtils {

    public static <T extends UserContext<?>> List<T> getSortedContexts(KeycloakSession session, Class<T> context) {
        return session.getAllProviders(UserContext.class)
                .stream()
                .filter(f -> context.isAssignableFrom(f.getClass()))
                .map(f -> (T) f)
                .sorted(Comparator.comparingInt(f -> ((UserContext<?>) f).getPriority()).reversed())
                .toList();
    }

    public static <T extends UserContext<?>> T getContext(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContext.class, providerId);
    }

    public static <T extends UserContextCondition> T getContextCondition(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContextCondition.class, providerId);
    }


}
