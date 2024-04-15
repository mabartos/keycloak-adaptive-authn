package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.adaptive.spi.policy.UserContextCondition;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

public class ContextUtils {

    public static <T extends UserContext<?>> T getContext(KeycloakSession session, Class<T> clazz) {
        return getContext(session, clazz, null);
    }

    public static <T extends UserContext<?>> T getContext(KeycloakSession session, Class<T> clazz, String contextProviderId) {
        var sf = session.getKeycloakSessionFactory();
        ProviderFactory<T> factory = contextProviderId == null ? sf.getProviderFactory(clazz) : sf.getProviderFactory(clazz, contextProviderId);
        if (factory == null) {
            throw new IllegalArgumentException("Provider factory does not exist");
        }
        T context = factory.create(session);
        if (context == null) {
            throw new IllegalArgumentException("Cannot create user context.");
        }
        return context;
    }

    public static <T extends UserContextCondition> T getContextCondition(KeycloakSession session, Class<T> clazz, String contextProviderId) {
        var sf = session.getKeycloakSessionFactory();
        ProviderFactory<T> factory = contextProviderId == null ? sf.getProviderFactory(clazz) : sf.getProviderFactory(clazz, contextProviderId);
        if (factory == null) {
            throw new IllegalArgumentException("Provider factory does not exist");
        }
        T context = factory.create(session);
        if (context == null) {
            throw new IllegalArgumentException("Cannot create user context condition.");
        }
        return context;
    }
}
