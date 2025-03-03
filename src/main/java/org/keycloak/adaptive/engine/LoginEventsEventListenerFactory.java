package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class LoginEventsEventListenerFactory implements EventListenerProviderFactory {
    public static final String PROVIDER_ID = "login-events-adaptive-authn";
    public static final String RISK_SCORE_DETAIL = "risk-score";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LoginEventsEventListener(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }
}
