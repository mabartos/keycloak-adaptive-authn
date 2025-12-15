package io.github.mabartos.engine;

import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.List;

import static io.github.mabartos.engine.LoginEventsEventListenerFactory.RISK_SCORE_DETAIL;
import static io.github.mabartos.spi.engine.RiskEngine.DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES;

public class LoginEventsEventListener implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(LoginEventsEventListener.class);

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
        var riskEngine = session.getProvider(RiskEngine.class);
        if (riskEngine != null && !riskEngine.isRiskBasedAuthnEnabled()) {
            log.debug("Risk-based authentication is disabled for this realm.");
            return;
        }

        switch (event.getType()) {
            case LOGIN -> handleLogin(event);
            case LOGOUT -> handleLogout(event);
        }
    }

    protected void handleLogin(Event event) {
        riskProvider.printStoredRisk().ifPresent(risk -> {
            // does not persist AFAIK
            event.getDetails().put(RISK_SCORE_DETAIL, risk);
            log.tracef("Added risk score ('%s') to the login session", risk);
        });

        var realmId = event.getRealmId();
        var userId = event.getUserId();

        if (StringUtil.isNotBlank(realmId) && StringUtil.isNotBlank(userId)) {
            var realm = session.realms().getRealm(realmId);
            if (realm == null) {
                log.warnf("Realm with realm ID '%s' does not exist, so no timer cannot be created.", realmId);
                return;
            }

            var user = session.users().getUserById(realm, userId);
            if (user == null) {
                log.warnf("User with user ID '%s' does not exist, so no timer cannot be created.", userId);
                return;
            }

            var timerScheduled = user.getFirstAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
            if (!Boolean.parseBoolean(timerScheduled)) {
                timerProvider.scheduleTask(new ScheduledContinuousRiskEvaluation(realmId, userId),
                        Duration.ofMinutes(DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES).toMillis(),
                        getUserTimerName(userId));
                user.setAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET, List.of("true"));
                log.debugf("Scheduled task for continuous risk evaluation was set. (User ID: '%s', period in minutes: '%d'", userId, DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES);
            }
        }
    }

    private static class ScheduledContinuousRiskEvaluation implements ScheduledTask {
        private final String realmId;
        private final String userId;

        public ScheduledContinuousRiskEvaluation(String realmId, String userId) {
            this.realmId = realmId;
            this.userId = userId;
        }

        @Override
        public void run(KeycloakSession session) {
            var riskEngine = session.getProvider(RiskEngine.class);
            var realm = session.realms().getRealm(realmId);
            var user = session.users().getUserById(realm, userId);
            riskEngine.evaluateRisk(RiskEvaluator.EvaluationPhase.CONTINUOUS, realm, user);
        }
    }

    protected void handleLogout(Event event) {
        if (StringUtil.isNotBlank(event.getUserId())) {
            var user = session.users().getUserById(session.getContext().getRealm(), event.getUserId());
            if (user == null) {
                log.warnf("User with user ID '%s' does not exist, so no timer cannot be created.", event.getUserId());
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
