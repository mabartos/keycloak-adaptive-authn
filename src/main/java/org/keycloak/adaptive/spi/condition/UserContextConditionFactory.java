package org.keycloak.adaptive.spi.condition;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.List;
import java.util.Set;

public abstract class UserContextConditionFactory<T extends UserContext<?>> implements ConditionalAuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private Set<Operation<T>> rules;

    @Override
    public void init(Config.Scope config) {
        this.rules = initRules();
    }

    abstract public Set<Operation<T>> initRules();

    public Set<Operation<T>> getRules() {
        return rules;
    }

    public List<String> getRulesTexts() {
        return getRules().stream().map(Operation::getText).toList();
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
