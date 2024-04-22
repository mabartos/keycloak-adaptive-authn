package org.keycloak.adaptive.spi.policy;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface AuthnPolicyProviderFactory extends ProviderFactory<AuthnPolicyProvider>, EnvironmentDependentProviderFactory {

    AuthnPolicyProvider create(KeycloakSession session, RealmModel realm);

    @Override
    default boolean isSupported(Config.Scope config) {
        //return Profile.isFeatureEnabled(Profile.Feature.AUTHN_POLICY); // TODO feature
        return true;
    }

}
