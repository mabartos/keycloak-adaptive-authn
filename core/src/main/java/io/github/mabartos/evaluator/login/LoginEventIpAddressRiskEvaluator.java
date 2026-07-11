package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Evaluates risk based on IP address history.
 * Known IPs = trust signal, unknown IPs = risk signal
 */
@EvaluationPhase(USER_KNOWN)
public class LoginEventIpAddressRiskEvaluator extends AbstractRiskEvaluator {
    private final LoginEventsContext loginEventsContext;
    private final DeviceRepresentationContext deviceContext;

    public LoginEventIpAddressRiskEvaluator(KeycloakSession session) {
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContext.class);
    }

    LoginEventIpAddressRiskEvaluator(
            LoginEventsContext loginEventsContext, DeviceRepresentationContext deviceContext) {
        this.loginEventsContext = loginEventsContext;
        this.deviceContext = deviceContext;
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

        return evaluateIpHistory(realm, numberOccurrences, events.size());
    }

    protected Risk evaluateIpHistory(RealmModel realm, long numberOccurrences, int eventsSize) {
        if (numberOccurrences == 0) {
            return Risk.of(
                    LoginEventIpAddressEvaluatorConfig.neverSeenIpScore(realm), "IP address never seen before");
        }
        if (eventsSize >= LoginEventIpAddressEvaluatorConfig.minLoginEvents(realm)) {
            int divisor = LoginEventIpAddressEvaluatorConfig.frequentIpThresholdDivisor(realm);
            long threshold = eventsSize / divisor;
            if (numberOccurrences >= threshold) {
                return Risk.of(
                        LoginEventIpAddressEvaluatorConfig.frequentIpScore(realm),
                        "Frequently used IP address - trust signal");
            }
            return Risk.of(
                    LoginEventIpAddressEvaluatorConfig.occasionalIpScore(realm), "IP address seen occasionally");
        }
        return Risk.invalid("Not enough login history");
    }
}
