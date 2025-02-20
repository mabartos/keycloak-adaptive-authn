package org.keycloak.adaptive.events;

import io.quarkus.logging.Log;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import static org.keycloak.adaptive.events.LoginEventsEventListenerFactory.RISK_SCORE_DETAIL;

public class LoginEventsEventListener implements EventListenerProvider {
    private final KeycloakSession session;
    private final StoredRiskProvider riskProvider;

    public LoginEventsEventListener(KeycloakSession session) {
        this.session = session;
        this.riskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN) {
            return;
        }

        riskProvider.printStoredRisk().ifPresent(risk -> {
            event.getDetails().put(RISK_SCORE_DETAIL, risk);
            Log.debugf("Added risk score ('%s') to the login session", risk);
        });
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {

    }

    @Override
    public void close() {

    }
}
