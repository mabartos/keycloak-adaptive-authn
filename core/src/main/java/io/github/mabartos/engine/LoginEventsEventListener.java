package io.github.mabartos.engine;

import io.github.mabartos.spi.context.UserContext;
import io.github.mabartos.spi.engine.OnSuccessfulLoginCallback;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.List;

import static io.github.mabartos.spi.engine.RiskEngine.DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES;

public class LoginEventsEventListener implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(LoginEventsEventListener.class);

    protected static final String USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET = "adaptive-engine-continuousTimerSet";
    private final KeycloakSession session;
    private final TimerProvider timerProvider;

    public LoginEventsEventListener(KeycloakSession session) {
        this.session = session;
        this.timerProvider = session.getProvider(TimerProvider.class);
    }

    @Override
    public void onEvent(Event event) {
        var realmId = event.getRealmId();
        if (StringUtil.isBlank(realmId)) return;

        var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            log.warnf("Realm with ID '%s' does not exist.", realmId);
            return;
        }

        var riskEngine = session.getProvider(RiskEngine.class);
        if (riskEngine != null && !riskEngine.isRiskBasedAuthnEnabled(realm)) {
            log.debug("Risk-based authentication is disabled for this realm.");
            return;
        }

        switch (event.getType()) {
            case LOGIN -> handleLogin(event, realm);
            case LOGOUT -> handleLogout(event, realm);
        }
    }

    protected void handleLogin(Event event, RealmModel realm) {
        var userId = event.getUserId();
        if (StringUtil.isBlank(userId)) return;

        var user = session.users().getUserById(realm, userId);
        if (user == null) {
            log.warnf("User with user ID '%s' does not exist, so no timer cannot be created.", userId);
            return;
        }

        session.getAllProviders(UserContext.class)
                .stream()
                .filter(context -> context instanceof OnSuccessfulLoginCallback)
                .forEach(context -> ((OnSuccessfulLoginCallback) context).onSuccessfulLogin(realm, user));
 
        String timerScheduled = String.valueOf(false);
        if (user.isFederated()) {
            timerScheduled = userFederatedStorage().getAttributes(realm, user.getId())
                    .getFirst(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
        } else {
            timerScheduled = user.getFirstAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
        }
        if (!Boolean.parseBoolean(timerScheduled)) {
            timerProvider.scheduleTask(new ScheduledContinuousRiskEvaluation(realm.getId(), userId),
                    Duration.ofMinutes(DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES).toMillis(),
                    getUserTimerName(userId));
            if (user.isFederated()) {
                userFederatedStorage().setAttribute(realm, user.getId(), USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET, List.of("true"));
            } else {
                user.setAttribute(USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET, List.of("true"));
            }
            log.debugf("Scheduled task for continuous risk evaluation was set. (User ID: '%s', period in minutes: '%d'", userId, DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES);
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
            session.getContext().setRealm(realm);
            var user = session.users().getUserById(realm, userId);
            riskEngine.evaluateRisk(RiskEvaluator.EvaluationPhase.CONTINUOUS, realm, user);
        }
    }

    protected void handleLogout(Event event, RealmModel realm) {
        var userId = event.getUserId();
        if (StringUtil.isBlank(userId)) return;

        var user = session.users().getUserById(realm, userId);
        if (user == null) {
            log.warnf("User with user ID '%s' does not exist, so no timer cannot be cancelled.", userId);
            return;
        }
        timerProvider.cancelTask(getUserTimerName(userId));
        if (user.isFederated()) {
            userFederatedStorage().removeAttribute(realm, user.getId(), USER_ATTRIBUTE_CONTINUOUS_EVALUATIONS_TIMER_SET);
        } else {
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

    private UserFederatedStorageProvider userFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }
}
