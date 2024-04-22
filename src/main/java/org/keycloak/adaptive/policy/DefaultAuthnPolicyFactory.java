package org.keycloak.adaptive.policy;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProviderFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;

public class DefaultAuthnPolicyFactory implements AuthnPolicyProviderFactory {
    private static final Logger logger = Logger.getLogger(DefaultAuthnPolicyFactory.class);
    public static final String PROVIDER_ID = "default-authn-policy";

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
        if (event instanceof RealmModel.RealmPostCreateEvent realmEvent) {
            logger.debugf("Handling RealmPostCreateEvent");
            configureAuthenticationFlows(realmEvent.getKeycloakSession(), realmEvent.getCreatedRealm());
        }

        if (event instanceof RealmModel.RealmRemovedEvent realmEvent) {
            logger.debugf("Handling RealmRemovedEvent");
            AuthnPolicyProvider provider = realmEvent.getKeycloakSession().getProvider(AuthnPolicyProvider.class);
            provider.removeAll();
        }
    }

    private void configureAuthenticationFlows(KeycloakSession session, RealmModel realm) {
        AuthenticationFlowModel browserFlow = realm.getBrowserFlow();

        if (browserFlow == null) {
            return;
        }

        if (realm.getAuthenticationExecutionsStream(browserFlow.getId())
                .map(AuthenticationExecutionModel::getAuthenticator)
                .anyMatch(DefaultAuthnPolicyFactory.PROVIDER_ID::equals)) {
            return;
        }

        addWrapperCondition(session, realm, browserFlow.getId(), 0, false);
        addWrapperCondition(session, realm, browserFlow.getId(), 9999, true);
    }

    protected void addWrapperCondition(KeycloakSession session, RealmModel realm, String parentFlowId, int priority, boolean requiresUser) {
        AuthenticationFlowModel policies = new AuthenticationFlowModel();
        policies.setTopLevel(false);
        policies.setBuiltIn(true);
        policies.setAlias("Authentication Policies");
        policies.setDescription("Set of Authentication policies");
        policies.setProviderId(AuthenticationPolicyFlow.BASIC_FLOW);
        policies = realm.addAuthenticationFlow(policies);

        final AuthenticationExecutionModel authnPolicies = new AuthenticationExecutionModel();
        authnPolicies.setParentFlow(parentFlowId);
        authnPolicies.setFlowId(policies.getId());
        authnPolicies.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        authnPolicies.setPriority(priority);
        authnPolicies.setAuthenticatorFlow(true);

        final AuthenticationExecutionModel parentFlow = realm.addAuthenticatorExecution(authnPolicies);
        final AuthnPolicyProviderFactory factory = (AuthnPolicyProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthnPolicyProvider.class);
        final AuthnPolicyProvider provider = factory.create(session, realm);

        provider.getAllStream(requiresUser)
                .forEach(f -> {
                    var execs = realm.getAuthenticationExecutionsStream(f.getId());
                    execs.forEach(g -> {
                        g.setParentFlow(parentFlow.getId());
                        realm.updateAuthenticatorExecution(g);
                    });
                });
    }
}
