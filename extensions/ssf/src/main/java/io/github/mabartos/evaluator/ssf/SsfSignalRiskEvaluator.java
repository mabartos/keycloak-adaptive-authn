package io.github.mabartos.evaluator.ssf;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ssf.SsfSignalContext;
import io.github.mabartos.context.ssf.SsfSignalData;
import io.github.mabartos.context.ssf.SsfSignalType;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.Risk.Score;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import static io.github.mabartos.spi.level.Risk.Score.EXTREME;
import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;

/**
 * Evaluates risk based on Shared Signals Framework (SSF) events received for the user.
 * <p>
 * SSF signals indicate security-relevant events from external identity providers or internal
 * Keycloak processing — credential compromise, session revocations, account takeover attempts,
 * device compliance changes, etc.
 * <p>
 * Risk is scored by combining signal type severity with recency:
 * <pre>
 * Signal Type                  | Within 1h  | 1-24h     | 1-7d   | Older
 * -----------------------------|------------|-----------|--------|--------
 * ACCOUNT_RECOVERY_ACTIVATED   | EXTREME    | VERY_HIGH | HIGH   | MEDIUM
 * CREDENTIAL_REVOKED           | VERY_HIGH  | HIGH      | MEDIUM | SMALL
 * ACCOUNT_DISABLED             | VERY_HIGH  | HIGH      | MEDIUM | SMALL
 * SESSION_REVOKED              | HIGH       | MEDIUM    | SMALL  | VERY_SMALL
 * CREDENTIAL_CHANGE            | MEDIUM     | SMALL     | VERY_SMALL | NONE
 * ASSURANCE_LEVEL_DECREASED    | MEDIUM     | SMALL     | VERY_SMALL | NONE
 * DEVICE_COMPLIANCE_CHANGED    | MEDIUM     | SMALL     | VERY_SMALL | NONE
 * </pre>
 * Multiple signals (3+ within 24h) bump the final score by one level (capped at EXTREME).
 */
@EvaluationPhase(USER_KNOWN)
public class SsfSignalRiskEvaluator extends AbstractRiskEvaluator {

    private static final Logger logger = Logger.getLogger(SsfSignalRiskEvaluator.class);

    private static final long ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1);
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final long SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7);
    private static final int FREQUENCY_THRESHOLD = 3;

    private final SsfSignalContext signalContext;

    public SsfSignalRiskEvaluator(KeycloakSession session) {
        this.signalContext = UserContexts.getContext(session, SsfSignalContext.class);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        List<SsfSignalData> signals = signalContext.getData(realm, knownUser).orElse(null);
        if (signals == null) {
            return Risk.invalid("Cannot retrieve SSF signals");
        }

        if (signals.isEmpty()) {
            return Risk.of(NEGATIVE_LOW, "No SSF signals — clean history");
        }

        long now = System.currentTimeMillis();
        Risk worstRisk = Risk.of(NONE);

        long recentSignalCount = 0;

        for (SsfSignalData signal : signals) {
            long ageMs = now - signal.timestamp();
            if (ageMs < ONE_DAY_MS) {
                recentSignalCount++;
            }

            Score score = scoreSignal(signal.type(), ageMs);
            if (score == Score.INVALID) {
                continue;
            }

            String reason = formatReason(signal, ageMs);
            worstRisk = worstRisk.max(Risk.of(score, reason));
        }

        if (recentSignalCount >= FREQUENCY_THRESHOLD && worstRisk.isValid() && worstRisk.getScore() != EXTREME) {
            Score bumped = bumpScore(worstRisk.getScore());
            worstRisk = Risk.of(bumped,
                    String.format("%d SSF signals within 24h — elevated risk", recentSignalCount));
        }

        logger.debugf("User %s: %d SSF signals, %d within 24h. Risk: %s",
                knownUser.getId(), signals.size(), recentSignalCount, worstRisk.getScore());

        return worstRisk;
    }

    private Score scoreSignal(SsfSignalType type, long ageMs) {
        return switch (type) {
            case ACCOUNT_RECOVERY_ACTIVATED -> ageMs < ONE_HOUR_MS ? EXTREME
                    : ageMs < ONE_DAY_MS ? VERY_HIGH
                    : ageMs < SEVEN_DAYS_MS ? HIGH : MEDIUM;

            case CREDENTIAL_REVOKED, ACCOUNT_DISABLED -> ageMs < ONE_HOUR_MS ? VERY_HIGH
                    : ageMs < ONE_DAY_MS ? HIGH
                    : ageMs < SEVEN_DAYS_MS ? MEDIUM : SMALL;

            case SESSION_REVOKED -> ageMs < ONE_HOUR_MS ? HIGH
                    : ageMs < ONE_DAY_MS ? MEDIUM
                    : ageMs < SEVEN_DAYS_MS ? SMALL : VERY_SMALL;

            case CREDENTIAL_CHANGE, ASSURANCE_LEVEL_DECREASED, DEVICE_COMPLIANCE_CHANGED ->
                    ageMs < ONE_HOUR_MS ? MEDIUM
                            : ageMs < ONE_DAY_MS ? SMALL
                            : ageMs < SEVEN_DAYS_MS ? VERY_SMALL : NONE;
        };
    }

    private String formatReason(SsfSignalData signal, long ageMs) {
        String age;
        if (ageMs < ONE_HOUR_MS) {
            age = String.format("%d min ago", TimeUnit.MILLISECONDS.toMinutes(ageMs));
        } else if (ageMs < ONE_DAY_MS) {
            age = String.format("%d hours ago", TimeUnit.MILLISECONDS.toHours(ageMs));
        } else {
            age = String.format("%d days ago", TimeUnit.MILLISECONDS.toDays(ageMs));
        }

        String source = signal.source() != null ? " from " + signal.source() : "";
        return String.format("SSF %s%s (%s)", signal.type().name(), source, age);
    }

    private Score bumpScore(Score current) {
        Score[] values = Score.values();
        int nextOrdinal = current.ordinal() + 1;
        if (nextOrdinal >= values.length) {
            return current;
        }
        return values[nextOrdinal];
    }
}
