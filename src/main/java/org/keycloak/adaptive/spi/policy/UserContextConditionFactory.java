package org.keycloak.adaptive.spi.policy;

import org.keycloak.Config;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Set;

public abstract class UserContextConditionFactory<T extends UserContext<?>> implements ConditionalAuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private Set<Operation<T>> rules;

    public T createContext(KeycloakSession session, String providerId) {
        final var context = session.getKeycloakSessionFactory()
                .getProviderFactory(UserContext.class, providerId);

        if (context == null) {
            throw new RuntimeException(String.format("Cannot find '%s' provider factory", providerId));
        }

        return (T) context.create(session);
    }

    @Override
    public void init(Config.Scope config) {
        this.rules = initRules();
    }

    abstract public Set<Operation<T>> initRules();

    public Set<Operation<T>> getRules() {
        return rules;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return null;
    }

}
