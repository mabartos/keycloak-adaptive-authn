package org.keycloak.adaptive.evaluator.login;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.user.KcLoginEventsContextFactory;
import org.keycloak.adaptive.context.user.LoginEventsContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AiTimeAccessRiskEvaluator extends AbstractRiskEvaluator {
    private final static Logger logger = Logger.getLogger(AiTimeAccessRiskEvaluator.class);
    private final KeycloakSession session;
    private final LoginEventsContext loginEvents;
    private final AiNlpEngine aiEngine;

    public AiTimeAccessRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.loginEvents = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiNlpEngine.class);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    protected String request(String currentTime, List<String> accessTimes) {
        var request = String.format("""
                        The list below represents times when user logged into application.
                        Rules:
                        - Give LOW(0.1-0.3) risk score when it is common user accessing the application this day in this time
                        - Give MEDIUM(0.5-0.7) risk score when user breaks its pattern and accessing the time in unusual time and day
                        - Give HIGH(0.9) risk score when user accessing the application in the middle of night, or in unusual time at weekend.                 
                                    
                        Current time: %s                        
                        Access times:
                        %s                                                 
                        """,
                currentTime,
                String.join("\n", accessTimes)
        );

        logger.debugf("AI time access request: %s", request);
        return request;
    }

    @Override
    public Risk evaluate() {
        if (loginEvents == null) {
            return Risk.invalid();
        }

        var dataOptional = loginEvents.getData();
        if (dataOptional.isEmpty()) {
            return Risk.invalid();
        }

        var accessTimes = dataOptional.get().stream()
                .filter(f -> f.getType() == EventType.LOGIN)
                .map(time -> getFormattedTime(time.getTime()))
                .toList();

        var currentTime = getFormattedTime(Time.currentTimeMillis());

        if (accessTimes.isEmpty()) {
            return Risk.none();
        }

        if (accessTimes.size() < 5) {
            return Risk.none();
        }

        return aiEngine.getRisk(request(currentTime, accessTimes))
                .map(Risk::of)
                .orElse(Risk.invalid());
    }

    // TODO we should take into account time zone of the user
    public String getFormattedTime(long timeInMillis) {
        return Optional.of(Instant.ofEpochMilli(timeInMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .map(time -> String.format("%s:%s", time, time.getDayOfWeek().toString()))
                .orElse(null);
    }
}
