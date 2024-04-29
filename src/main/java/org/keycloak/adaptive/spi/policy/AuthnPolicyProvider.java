package org.keycloak.adaptive.spi.policy;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.provider.Provider;

import java.util.Optional;
import java.util.stream.Stream;

public interface AuthnPolicyProvider extends Provider {
    String POLICY_PREFIX = "POLICY - ";

    AuthenticationFlowModel getParentPolicy();

    AuthenticationFlowModel getOrCreateParentPolicy();

    AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy);

    AuthenticationFlowModel addPolicy(AuthenticationFlowModel policy, String parentFlowId);

    Stream<AuthenticationFlowModel> getAllStream();

    Stream<AuthenticationFlowModel> getAllStream(boolean requiresUser);

    Optional<AuthenticationFlowModel> getById(String id);

    Optional<AuthenticationFlowModel> getByAlias(String alias);

    boolean remove(AuthenticationFlowModel policy);

    void removeAll();

    void update(AuthenticationFlowModel policy);
}
