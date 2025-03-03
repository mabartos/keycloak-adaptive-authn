package org.keycloak.adaptive.engine;

import io.quarkus.logging.Log;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.StoredRiskProvider;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.TimerProvider;
import org.keycloak.utils.StringUtil;

import java.util.List;

import static org.keycloak.adaptive.engine.LoginEventsEventListenerFactory.RISK_SCORE_DETAIL;

public class LoginEventsEventListener implements EventListenerProvider {
    protected static final String USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET = "has_continuous_evaluations_timer_set";

    private final KeycloakSession session;
    private final StoredRiskProvider riskProvider;
    private final TimerProvider timerProvider;

    public LoginEventsEventListener(KeycloakSession session) {
        this.session = session;
        this.riskProvider = session.getProvider(StoredRiskProvider.class);
        this.timerProvider = session.getProvider(TimerProvider.class);
    }

    @Override
    public void onEvent(Event event) {
        switch (event.getType()) {
            case LOGIN -> handleLogin(event);
            case LOGOUT -> handleLogout(event);
        }
    }

    protected void handleLogin(Event event) {
        riskProvider.printStoredRisk().ifPresent(risk -> {
            // does not persist AFAIK
            event.getDetails().put(RISK_SCORE_DETAIL, risk);
            Log.debugf("Added risk score ('%s') to the login session", risk);
        });

        var userId = event.getUserId();
        if (StringUtil.isNotBlank(userId)) {
            var user = session.users().getUserById(session.getContext().getRealm(), userId);
            if (user == null) {
                Log.warnf("User with user ID '%s' does not exist, so no timer cannot be created.", userId);
                return;
            }

            var timerScheduled = user.getFirstAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
            if (!Boolean.parseBoolean(timerScheduled)) {
                timerProvider.scheduleTask(session -> {
                    var riskEngine = session.getProvider(RiskEngine.class);
                    riskEngine.evaluateRisk(RiskEvaluator.EvaluationPhase.CONTINUOUS);
                }, 5000, getUserTimerName(userId)); //TODO change the time
                user.setAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET, List.of("true"));
            }
        }
    }

    protected void handleLogout(Event event) {
        if (StringUtil.isNotBlank(event.getUserId())) {
            var user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user == null) {
                Log.warnf("User with user ID '%s' does not exist, so no timer cannot be created.", event.getUserId());
                return;
            }
            timerProvider.cancelTask(getUserTimerName(event.getId()));
            user.removeAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
        }
    }

    protected static String getUserTimerName(String userId) {
        return String.format("risk-evaluators-continuous-user-%s", userId);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {

    }

    @Override
    public void close() {

    }
}
