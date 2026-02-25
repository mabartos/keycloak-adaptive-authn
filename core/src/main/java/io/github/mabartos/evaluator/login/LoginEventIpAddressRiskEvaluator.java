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

import static io.github.mabartos.level.Risk.INTERMEDIATE;
import static io.github.mabartos.level.Risk.SMALL;
import static io.github.mabartos.level.Risk.VERY_HIGH;

public class LoginEventIpAddressRiskEvaluator extends AbstractRiskEvaluator {
    private final LoginEventsContext loginEventsContext;
    private final DeviceRepresentationContext deviceContext;

    public LoginEventIpAddressRiskEvaluator(KeycloakSession session) {
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContextFactory.PROVIDER_ID);
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
            return Risk.of(VERY_HIGH);
        } else {
            var eventsSize = events.size();
            if (eventsSize > 3) {
                long threshold = eventsSize / 3;
                if (numberOccurrences >= threshold) {
                    return Risk.of(SMALL);
                } else {
                    return Risk.of(INTERMEDIATE);
                }
            } else {
                return Risk.none();
            }
        }

    }
}
