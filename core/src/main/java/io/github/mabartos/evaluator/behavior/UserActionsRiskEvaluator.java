package io.github.mabartos.evaluator.behavior;

import org.jboss.logging.Logger;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.evaluator.AbstractContinuousRiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.Date;

import static org.keycloak.events.EventType.DELETE_ACCOUNT;
import static org.keycloak.events.EventType.DELETE_ACCOUNT_ERROR;
import static org.keycloak.events.EventType.REMOVE_CREDENTIAL;
import static org.keycloak.events.EventType.REMOVE_CREDENTIAL_ERROR;
import static org.keycloak.events.EventType.RESET_PASSWORD;
import static org.keycloak.events.EventType.RESET_PASSWORD_ERROR;
import static org.keycloak.events.EventType.UPDATE_CREDENTIAL;
import static org.keycloak.events.EventType.UPDATE_CREDENTIAL_ERROR;
import static org.keycloak.events.EventType.UPDATE_EMAIL;
import static org.keycloak.events.EventType.UPDATE_EMAIL_ERROR;

public class UserActionsRiskEvaluator extends AbstractContinuousRiskEvaluator {
    protected static final EventType[] SENSITIVE_EVENTS = {
            UPDATE_EMAIL,
            UPDATE_EMAIL_ERROR,
            RESET_PASSWORD,
            RESET_PASSWORD_ERROR,
            DELETE_ACCOUNT,
            DELETE_ACCOUNT_ERROR,
            UPDATE_CREDENTIAL,
            UPDATE_CREDENTIAL_ERROR,
            REMOVE_CREDENTIAL,
            REMOVE_CREDENTIAL_ERROR
    };
    private static final Logger logger = Logger.getLogger(UserActionsRiskEvaluator.class);
    private static final long COEFFICIENT_BASE = Duration.ofMinutes(10).toMillis();

    private final KeycloakSession session;
    private final EventStoreProvider eventStore;

    public UserActionsRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.eventStore = session.getProvider(EventStoreProvider.class);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    /**
     * Number of occurrences of sensitive events in specific time window
     * <p>
     * In 10 minutes
     * 1x   = NONE
     * 2x   = SMALL
     * 3x   = MEDIUM
     * 5x   = INTERMEDIATE
     * 10x  = VERY_HIGH
     * >10x = HIGHEST
     * <p>
     * Coefficient is proportional to 10 minutes, so if the lookupTime is 30min, the coefficient will be 3
     * <p>
     * f.e. for 30 min
     * c=3
     * <p>
     * c*1x  = NONE
     * c*2x  = SMALL
     * c*3x  = MEDIUM
     * c*5x  = INTERMEDIATE
     * c*10x = VERY_HIGH
     * >c*10x= HIGHEST
     */
    @Override
    public Risk evaluate(RealmModel realm, UserModel user) {
        if (realm == null) {
            return Risk.invalid("Cannot find realm");
        }

        if (user == null) {
            return Risk.invalid("Cannot find user");
        }


        // TODO have it configurable
        var lookupTime = Duration.ofMinutes(RiskEngine.DEFAULT_CONTINUOUS_RISK_EVALUATION_PERIOD_MINUTES).toMillis();

        long coefficient = 1;
        if (lookupTime > COEFFICIENT_BASE) {
            coefficient = lookupTime / COEFFICIENT_BASE;
        }

        var fromDate = new Date(Time.currentTimeMillis() - lookupTime);

        var count = eventStore.createQuery()
                .realm(realm.getId())
                .user(user.getId())
                .type(SENSITIVE_EVENTS)
                .fromDate(fromDate)
                .getResultStream()
                .toList()
                .size();

        if (count == 0) {
            return Risk.none();
        } else if (count <= 2 * coefficient) {
            return Risk.of(Risk.SMALL);
        } else if (count <= 3 * coefficient) {
            return Risk.of(Risk.MEDIUM);
        } else if (count <= 5 * coefficient) {
            return Risk.of(Risk.INTERMEDIATE);
        } else if (count <= 10 * coefficient) {
            return Risk.of(Risk.VERY_HIGH);
        } else {
            return Risk.of(Risk.HIGHEST);
        }
    }
}
