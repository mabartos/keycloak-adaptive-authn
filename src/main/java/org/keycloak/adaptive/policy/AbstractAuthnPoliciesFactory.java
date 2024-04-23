package org.keycloak.adaptive.policy;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.spi.policy.AuthnPolicyProvider;
import org.keycloak.authentication.EphemeralFlowFactory;
import org.keycloak.authentication.EphemeralFlowProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderEvent;

public abstract class AbstractAuthnPoliciesFactory implements EphemeralFlowFactory {
    private static final Logger logger = Logger.getLogger(AuthnPolicyRequiresUserFactory.class);

    public abstract boolean requiresUser();

    public abstract String getAlias();

    public abstract String getDescription();

    public abstract int getPriority();

    @Override
    public EphemeralFlowProvider create(KeycloakSession session) {
        configureAuthnPolicy(session.getContext().getRealm());
        return new AuthnPolicyEphemeralProvider(session, requiresUser(), getPriority());
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
    public boolean isSupported(Config.Scope config) {
        return false;//TODO add feature check
    }

    private void handleEvents(ProviderEvent event) {
        if (event instanceof RealmModel.RealmPostCreateEvent realmEvent) {
            logger.debugf("Handling RealmPostCreateEvent");
            configureAuthnPolicy(realmEvent.getCreatedRealm());
        }

        if (event instanceof RealmModel.RealmRemovedEvent realmEvent) {
            logger.debugf("Handling RealmRemovedEvent");
            AuthnPolicyProvider provider = realmEvent.getKeycloakSession().getProvider(AuthnPolicyProvider.class);
            provider.removeAll();
        }
    }

    protected void configureAuthnPolicy(RealmModel realm) {
        boolean flowExists = realm.getFlowByAlias(getAlias()) != null;
        if (flowExists) return;

        final AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        flow.setAlias(getAlias());
        flow.setDescription(getDescription());
        flow.setProviderId(AuthenticationPolicyFlow.BASIC_FLOW);
        realm.addAuthenticationFlow(flow);

        final AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setFlowId(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setPriority(getPriority());
        execution.setAuthenticatorFlow(true);
        execution.setParentFlow(realm.getBrowserFlow().getId());
        realm.addAuthenticatorExecution(execution);
    }
}
