package org.keycloak.adaptive.spi.policy;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.provider.Provider;

import java.util.stream.Stream;

public interface AuthnPolicyProvider extends Provider {

    AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy);

    Stream<AuthenticationFlowModel> getAllStream();

    Stream<AuthenticationFlowModel> getAllStream(boolean requiresUser);

    AuthenticationFlowModel getById(String id);

    boolean remove(AuthenticationFlowModel policy);

    void removeAll();
}
