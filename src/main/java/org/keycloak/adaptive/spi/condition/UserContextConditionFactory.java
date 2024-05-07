package org.keycloak.adaptive.spi.condition;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.List;

public abstract class UserContextConditionFactory<T extends UserContext<?>> implements VerifiableUserContextFactory<T>, ConditionalAuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private List<Operation<T>> operations;

    @Override
    public void init(Config.Scope config) {
        this.operations = initOperations();
    }

    @Override
    public List<Operation<T>> getOperations() {
        return operations;
    }

    @Override
    public List<String> getOperationsTexts() {
        return getOperations().stream().map(Operation::getText).toList();
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
