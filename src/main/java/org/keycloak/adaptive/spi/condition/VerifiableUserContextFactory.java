package org.keycloak.adaptive.spi.condition;

import org.keycloak.adaptive.spi.context.UserContext;

import java.util.List;

public interface VerifiableUserContextFactory<T extends UserContext<?>> {

    List<Operation<T>> initOperations();

    List<Operation<T>> getOperations();

    List<String> getOperationsTexts();
}
