package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.timer.TimerProvider;

public class LoginEventsEventListenerFactory implements EventListenerProviderFactory, ProviderEventListener {
    public static final String PROVIDER_ID = "login-events-adaptive-authn";
    public static final String RISK_SCORE_DETAIL = "risk-score";
    private Runnable onClose;

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
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public void close() {
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent pme) {
            setupScheduledTasks(pme.getFactory());
        }
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        try (KeycloakSession session = sessionFactory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            if (timer != null) {
                // TODO setup scheduled continuous evaluator
            }
        }
    }
}
