package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.adaptive.spi.policy.UserContextCondition;
import org.keycloak.models.KeycloakSession;

public class ContextUtils {

    public static <T extends UserContext<?>> T getContext(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContext.class, providerId);
    }

    public static <T extends UserContextCondition> T getContextCondition(KeycloakSession session, String providerId) {
        return (T) session.getProvider(UserContextCondition.class, providerId);
    }
}
