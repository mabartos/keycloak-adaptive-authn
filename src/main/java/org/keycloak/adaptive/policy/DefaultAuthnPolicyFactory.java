package org.keycloak.adaptive.policy;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    public static final String PROVIDER_ID = "default-authn-policy";

    @Override
    public AuthnPolicyProvider create(KeycloakSession session) {
        return new DefaultAuthnPolicyProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this::handleEvents);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private void handleEvents(ProviderEvent event) {
        if (event instanceof RealmModel.RealmPostCreateEvent) {
            RealmModel realm = ((RealmModel.RealmPostCreateEvent) event).getCreatedRealm();
            configureAuthenticationFlows(realm);
        }

        if (event instanceof RealmModel.RealmRemovedEvent) {
            KeycloakSession session = ((RealmModel.RealmRemovedEvent) event).getKeycloakSession();
            AuthnPolicyProvider provider = session.getProvider(AuthnPolicyProvider.class);
            provider.removeAll();
        }
    }

    private void configureAuthenticationFlows(RealmModel realm) {
        AuthenticationFlowModel browserFlow = realm.getBrowserFlow();

        if (browserFlow == null) {
            return;
        }

        if (realm.getAuthenticationExecutionsStream(browserFlow.getId())
                .map(AuthenticationExecutionModel::getAuthenticator)
                .anyMatch(DefaultAuthnPolicyFactory.PROVIDER_ID::equals)) {
            return;
        }

        AuthenticationExecutionModel authnPoliciesNoUser = new AuthenticationExecutionModel();
        authnPoliciesNoUser.setParentFlow(browserFlow.getId());
        authnPoliciesNoUser.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        authnPoliciesNoUser.setAuthenticator(DefaultAuthnPolicyFactory.PROVIDER_ID); // TODO
        authnPoliciesNoUser.setPriority(0); // First flow execution
        authnPoliciesNoUser.setAuthenticatorFlow(true);

        authnPoliciesNoUser = realm.addAuthenticatorExecution(authnPoliciesNoUser);

        /*

        authnPolicies.setAlias("Authentication Policies");
        authnPolicies.setDescription("Set of authentication policies");
        authnPolicies.setProviderId(AuthenticationFlow.BASIC_FLOW);
        authnPolicies.setTopLevel(false);
        authnPolicies.setBuiltIn(true);
        authnPolicies. (true);
        authnPolicies = realm.addAuthenticationFlow(authnPolicies);


        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();

        execution.setParentFlow(browserFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        execution.setAuthenticator(DefaultAuthnPolicyFactory.PROVIDER_ID);
        execution.setPriority(0);
        execution.setAuthenticatorFlow(false);

        realm.addAuthenticatorExecution(execution);*/
    }


}
