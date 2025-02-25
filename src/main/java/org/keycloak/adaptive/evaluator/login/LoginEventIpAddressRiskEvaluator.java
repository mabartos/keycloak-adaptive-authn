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

import java.util.Set;

import static org.keycloak.adaptive.level.Risk.INTERMEDIATE;
import static org.keycloak.adaptive.level.Risk.NONE;
import static org.keycloak.adaptive.level.Risk.SMALL;
import static org.keycloak.adaptive.level.Risk.VERY_HIGH;

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
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate() {
        if (deviceContext.getData().isEmpty()) {
            return Risk.invalid();
        }

        if (loginEvents.getData().isEmpty()) {
            return Risk.invalid();
        }

        var events = loginEvents.getData().get();

        var eventsSize = events.size();
        if (eventsSize == 0) {
            return Risk.invalid();
        }

        var device = deviceContext.getData().get();

        var numberOccurrences = events.stream()
                .map(Event::getIpAddress)
                .filter(f -> f.equals(device.getIpAddress()))
                .count();

        if (numberOccurrences == 0) {
            return Risk.of(VERY_HIGH);
        } else {
            if (eventsSize > 3) {
                long threshold = eventsSize / 3;
                if (numberOccurrences >= threshold) {
                    return Risk.of(SMALL);
                } else {
                    return Risk.of(INTERMEDIATE);
                }
            } else {
                return Risk.of(NONE);
            }
        }

    }
}
