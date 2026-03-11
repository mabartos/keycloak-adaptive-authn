package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

import static io.github.mabartos.level.Risk.Score.HIGH;
import static io.github.mabartos.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.level.Risk.Score.VERY_SMALL;

/**
 * Evaluates risk based on IP address history.
 * Known IPs = trust signal, unknown IPs = risk signal
 */
public class LoginEventIpAddressRiskEvaluator extends AbstractRiskEvaluator {
    private final LoginEventsContext loginEventsContext;
    private final DeviceRepresentationContext deviceContext;

    public LoginEventIpAddressRiskEvaluator(KeycloakSession session) {
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContext.class);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var deviceRepresentation = deviceContext.getData(realm, knownUser).orElse(null);
        if (deviceRepresentation == null) {
            return Risk.invalid("No device information");
        }

        var events = loginEventsContext.getData(realm, knownUser).orElse(null);
        if (events == null || events.isEmpty()) {
            return Risk.invalid("No login events");
        }

        var numberOccurrences = events.stream()
                .map(Event::getIpAddress)
                .filter(f -> f.equals(deviceRepresentation.getIpAddress()))
                .count();

        if (numberOccurrences == 0) {
            return Risk.of(HIGH, "IP address never seen before");
        } else {
            var eventsSize = events.size();
            if (eventsSize > 3) {
                long threshold = eventsSize / 3;
                if (numberOccurrences >= threshold) {
                    // Seen frequently - trust signal
                    return Risk.of(NEGATIVE_LOW, "Frequently used IP address - trust signal");
                } else {
                    // Seen sometimes but not frequently
                    return Risk.of(VERY_SMALL, "IP address seen occasionally");
                }
            } else {
                // Not enough data
                return Risk.invalid("Not enough login history");
            }
        }

    }
}
