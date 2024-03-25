package org.keycloak.adaptive.spi.policy;

import org.keycloak.provider.ConfiguredProvider;

import java.util.Collection;

public interface ConfiguredRuleProvider extends ConfiguredProvider {

    Collection<Operation> getOperations();

    interface Operation {
        String text();

        String symbol();
    }
}
