package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.user.TypicalAccessTimeContext;
import io.github.mabartos.context.user.TypicalAccessTimeContextFactory;
import io.github.mabartos.context.user.TypicalAccessTimeData;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Set;

import static io.github.mabartos.spi.level.Risk.Score.*;

/**
 * Evaluates risk based on unusual login times using circular statistics.
 * <p>
 * <strong>Overview:</strong><br>
 * This evaluator detects fraudulent access by identifying when users log in at unusual times.
 * For example, if a user normally logs in at 8-9am every day, a login at 3am on the weekend
 * is highly suspicious and may indicate account compromise.
 * <p>
 * <strong>How It Works:</strong>
 * <ol>
 * <li><strong>Circular EWMA Profile:</strong> Uses circular statistics (sine/cosine components)
 * to track typical login times. This handles the wraparound nature of time where 23:00 is
 * actually close to 01:00, not 22 hours away. Applies Exponentially Weighted Moving Average
 * to give more weight to recent patterns while retaining history.</li>
 *
 * <li><strong>Pattern Learning:</strong> Uses {@link TypicalAccessTimeContext} to retrieve the
 * time pattern profile, which is built from historical login data and stored in user attributes
 * ({@code timePattern.meanSin}, {@code timePattern.meanCos}). The context bootstraps the profile
 * from historical events if no saved profile exists. Requires 5+ logins before evaluating risk.</li>
 *
 * <li><strong>Risk Scoring:</strong> Calculates how many hours the current login deviates from
 * the user's typical pattern:
 * <ul>
 *   <li>&lt; 2 hours → VERY_SMALL (normal)</li>
 *   <li>2-4 hours → SMALL (slightly unusual)</li>
 *   <li>4-6 hours → MEDIUM (moderately unusual)</li>
 *   <li>6-8 hours → HIGH (very unusual)</li>
 *   <li>&gt; 8 hours → VERY_HIGH to EXTREME (opposite time of day)</li>
 * </ul>
 * Additional risk for midnight hours (2am-5am) increases severity.
 * </li>
 * </ol>
 * <p>
 * <strong>Real-World Example:</strong><br>
 * Office worker normally logs in at 08:00-10:00 on weekdays. Pattern learned: mean ~09:00.
 * <pre>
 * Login at 09:00 → VERY_SMALL risk (0h deviation) ✓ Normal
 * Login at 12:00 → SMALL risk (3h deviation) ⚠️ Lunch time, unusual
 * Login at 18:00 → HIGH risk (9h deviation) ⚠️⚠️ Evening, very unusual
 * Login at 03:00 → EXTREME risk (6h deviation + midnight) 🚨 Fraud suspect!
 * </pre>
 * <p>
 * <strong>User Attributes:</strong><br>
 * Time pattern data is managed by {@link TypicalAccessTimeContext} and shared with other evaluators.
 *
 * @see CircularEwmaProfile
 * @see TypicalAccessTimeContext
 * @see TypicalAccessTimeData
 */
public class TimePatternRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(TimePatternRiskEvaluator.class);

    private final TypicalAccessTimeContext typicalTimeContext;

    public TimePatternRiskEvaluator(KeycloakSession session) {
        this.typicalTimeContext = UserContexts.getContext(session, TypicalAccessTimeContext.class);
    }

    // Thresholds for risk scoring (in hours of deviation)
    private static final double THRESHOLD_SMALL = 2.0;
    private static final double THRESHOLD_MEDIUM = 4.0;
    private static final double THRESHOLD_HIGH = 6.0;
    private static final double THRESHOLD_VERY_HIGH = 8.0;

    // Minimum concentration to trust the pattern (0-1 scale)
    private static final double MIN_CONCENTRATION = 0.3;

    @Override
    public boolean allowRetries() {
        return false;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel user) {
        if (user == null) {
            return Risk.invalid("User is null");
        }

        if (typicalTimeContext == null) {
            return Risk.invalid("Cannot access typical time context");
        }

        // Get typical time data from context
        var timeDataOptional = typicalTimeContext.getData(realm, user);
        if (timeDataOptional.isEmpty()) {
            return Risk.invalid("Cannot retrieve typical time data");
        }

        TypicalAccessTimeData timeData = timeDataOptional.get();

        // Not enough data yet - can't evaluate risk
        if (!timeData.hasSufficientData()) {
            return Risk.invalid(String.format("Building time pattern (login %d/%d)",
                    timeData.getLoginCount(), TypicalAccessTimeContext.MIN_LOGINS));
        }

        LocalDateTime currentTime = Instant.ofEpochMilli(Time.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        int currentHour = currentTime.getHour();

        CircularEwmaProfile profile = timeData.getProfile();
        double concentration = timeData.getConcentration();

        // Check if we have a reliable pattern
        if (concentration < MIN_CONCENTRATION) {
            // User logs in at very different times - pattern is too dispersed to be useful
            return Risk.of(VERY_SMALL, String.format("User has irregular login times (concentration: %.2f)", concentration));
        }

        // Calculate deviation from normal pattern
        double deviation = profile.getDeviation(currentHour);
        double meanHour = timeData.getExactMeanHour();

        logger.debugf("User %s - Current: %02d:00, Mean: %02.1f, Deviation: %.2f hours, Concentration: %.2f",
                user.getUsername(), currentHour, meanHour, deviation, concentration);

        // Determine risk based on deviation
        return calculateRisk(currentHour, deviation, meanHour, concentration);
    }

    private Risk calculateRisk(int currentHour, double deviation, double meanHour, double concentration) {
        String reason = String.format("Login at %02d:00 (typical: %02.0f:00, deviation: %.1fh)",
                currentHour, meanHour, deviation);

        // Additional risk for middle-of-night logins (2am-5am)
        boolean isMidnightHours = currentHour >= 2 && currentHour <= 5;

        if (deviation < THRESHOLD_SMALL) {
            // Very close to normal pattern
            return Risk.of(VERY_SMALL, reason + " - typical time");
        } else if (deviation < THRESHOLD_MEDIUM) {
            // Slightly unusual but not too concerning
            return Risk.of(SMALL, reason + " - slightly unusual");
        } else if (deviation < THRESHOLD_HIGH) {
            // Moderately unusual
            if (isMidnightHours) {
                return Risk.of(HIGH, reason + " - unusual time in midnight hours");
            }
            return Risk.of(MEDIUM, reason + " - moderately unusual");
        } else if (deviation < THRESHOLD_VERY_HIGH) {
            // Very unusual
            if (isMidnightHours) {
                return Risk.of(VERY_HIGH, reason + " - very unusual time in midnight hours");
            }
            return Risk.of(HIGH, reason + " - very unusual time");
        } else {
            // Extremely unusual - opposite time of day
            if (isMidnightHours) {
                return Risk.of(EXTREME, reason + " - extreme deviation in midnight hours");
            }
            return Risk.of(VERY_HIGH, reason + " - extreme deviation from pattern");
        }
    }

}
