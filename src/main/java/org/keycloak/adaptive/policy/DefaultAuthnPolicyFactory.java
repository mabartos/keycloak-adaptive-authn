package org.keycloak.adaptive.policy;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import static org.keycloak.authentication.AuthenticationFlow.BASIC_FLOW;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    public static final String PROVIDER_ID = "default";
    public static final String DEFAULT_AUTHN_POLICIES_FLOW_ALIAS = "Authentication policies - PARENT";

    @Override
    public AuthnPolicyProvider create(KeycloakSession session) {
        return new DefaultAuthnPolicyProvider(session);
    }

    @Override
    public AuthnPolicyProvider create(KeycloakSession session, RealmModel realm) {
        return new DefaultAuthnPolicyProvider(session, realm);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    static AuthenticationFlowModel createParentFlow(RealmModel realm) {
        var parent = new AuthenticationFlowModel();
        parent.setAlias(DEFAULT_AUTHN_POLICIES_FLOW_ALIAS);
        parent.setDescription("Parent Authentication Policy");
        parent.setProviderId(BASIC_FLOW);
        parent.setTopLevel(true);
        parent.setBuiltIn(false);
        return realm.addAuthenticationFlow(parent);
    }
}
