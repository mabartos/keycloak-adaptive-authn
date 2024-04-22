package org.keycloak.adaptive.spi.policy;

import org.keycloak.adaptive.models.AuthnPolicyModel;
import org.keycloak.provider.Provider;

import java.util.stream.Stream;

public interface AuthnPolicyProvider extends Provider {

    AuthnPolicyModel addPolicy(AuthnPolicyModel policy);

    Stream<AuthnPolicyModel> getAllStream();

    Stream<AuthnPolicyModel> getAllStream(boolean requiresUser);

    AuthnPolicyModel getById(String id);

    boolean remove(AuthnPolicyModel policy);

    void removeAll();
}
