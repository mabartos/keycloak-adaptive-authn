package org.keycloak.adaptive.evaluator.login;

import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.device.DefaultDeviceContextFactory;
import org.keycloak.adaptive.context.device.DeviceContext;
import org.keycloak.adaptive.context.user.KcLoginEventsContextFactory;
import org.keycloak.adaptive.context.user.LoginEventsContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class LoginEventIpAddressRiskEvaluator extends AbstractRiskEvaluator {
    private final KeycloakSession session;
    private final LoginEventsContext loginEvents;
    private final DeviceContext deviceContext;

    public LoginEventIpAddressRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.loginEvents = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.deviceContext = UserContexts.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
    }

    @Override
    public boolean isEnabled(){
        // TODO - bug when authenticate multiple times - SQLException - Connection is closed
        return false;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public Optional<Double> evaluate() {
        return deviceContext.getData().flatMap(device -> loginEvents.getData().map(events -> {
            var eventsSize = events.size();
            if (eventsSize == 0) {
                return null;
            }

            var numberOccurrences = events.stream()
                    .map(Event::getIpAddress)
                    .filter(f -> f.equals(device.getIpAddress()))
                    .count();

            if (numberOccurrences == 0) {
                return Risk.VERY_HIGH;
            } else {
                if (eventsSize > 3) {
                    long threshold = eventsSize / 3;
                    if (numberOccurrences >= threshold) {
                        return Risk.SMALL;
                    } else {
                        return Risk.INTERMEDIATE;
                    }
                } else {
                    return Risk.NONE;
                }
            }
        }));
    }
}
