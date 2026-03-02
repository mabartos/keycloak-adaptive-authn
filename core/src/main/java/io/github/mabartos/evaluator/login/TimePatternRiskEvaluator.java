package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static io.github.mabartos.level.Risk.Score.*;

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
 * <li><strong>Pattern Learning:</strong> Reads historical login data from {@link LoginEventsContext}
 * and stores the pattern state in user attributes ({@code timePattern.meanSin}, {@code timePattern.meanCos}).
 * Bootstraps the profile from historical events if no saved profile exists. Requires 5+ logins
 * before evaluating risk.</li>
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
 * Stores {@code timePattern.meanSin} and {@code timePattern.meanCos} per user.
 * Login count is dynamically retrieved from {@link LoginEventsContext} for consistency.
 *
 * @see CircularEwmaProfile
 * @see LoginEventsContext
 */
public class TimePatternRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(TimePatternRiskEvaluator.class);

    private final LoginEventsContext loginEvents;

    // User attribute keys for storing profile state
    private static final String ATTR_MEAN_SIN = "timePattern.meanSin";
    private static final String ATTR_MEAN_COS = "timePattern.meanCos";

    // EWMA smoothing factor - lower values give more weight to history
    private static final double ALPHA = 0.15;

    public TimePatternRiskEvaluator(KeycloakSession session) {
        this.loginEvents = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
    }

    // Minimum logins required before we start evaluating risk
    private static final int MIN_LOGINS = 5;

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

        if (loginEvents == null) {
            return Risk.invalid("Cannot access login events");
        }

        // Get historical login events
        var dataOptional = loginEvents.getData(realm, user);
        if (dataOptional.isEmpty()) {
            return Risk.invalid("Cannot parse login events");
        }

        List<Event> events = dataOptional.get();
        List<Event> loginOnlyEvents = events.stream()
                .filter(f -> f.getType() == EventType.LOGIN)
                .toList();

        int loginCount = loginOnlyEvents.size();

        // Not enough data yet - can't evaluate risk
        if (loginCount < MIN_LOGINS) {
            return Risk.invalid(String.format("Building time pattern (login %d/%d)", loginCount, MIN_LOGINS));
        }

        LocalDateTime currentTime = Instant.ofEpochMilli(Time.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        int currentHour = currentTime.getHour();

        // Load existing profile from user attributes or bootstrap from history
        CircularEwmaProfile profile = loadOrBootstrapProfile(user, loginOnlyEvents);

        // Check if we have a reliable pattern
        double concentration = profile.getConcentration();
        if (concentration < MIN_CONCENTRATION) {
            // User logs in at very different times - pattern is too dispersed to be useful
            profile.update(currentHour);
            saveProfile(user, profile);
            return Risk.of(VERY_SMALL, String.format("User has irregular login times (concentration: %.2f)", concentration));
        }

        // Calculate deviation from normal pattern
        double deviation = profile.getDeviation(currentHour);
        double meanHour = profile.getMeanHour();

        logger.debugf("User %s - Current: %02d:00, Mean: %02.1f, Deviation: %.2f hours, Concentration: %.2f",
                user.getUsername(), currentHour, meanHour, deviation, concentration);

        // Determine risk based on deviation
        Risk risk = calculateRisk(currentHour, deviation, meanHour, concentration);

        // Update profile with current time
        profile.update(currentHour);
        saveProfile(user, profile);

        return risk;
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

    /**
     * Loads existing profile from user attributes, or bootstraps a new one from historical login data.
     */
    private CircularEwmaProfile loadOrBootstrapProfile(UserModel user, List<Event> loginEvents) {
        List<String> meanSinValues = user.getAttributeStream(ATTR_MEAN_SIN).toList();
        List<String> meanCosValues = user.getAttributeStream(ATTR_MEAN_COS).toList();

        // Try to load existing profile
        if (!meanSinValues.isEmpty() && !meanCosValues.isEmpty()) {
            try {
                double meanSin = Double.parseDouble(meanSinValues.get(0));
                double meanCos = Double.parseDouble(meanCosValues.get(0));
                return new CircularEwmaProfile(ALPHA, meanSin, meanCos);
            } catch (NumberFormatException e) {
                logger.warnf("Failed to parse time pattern for user %s, bootstrapping from history", user.getUsername());
            }
        }

        // No valid profile exists - bootstrap from historical login events
        logger.debugf("Bootstrapping time pattern for user %s from %d historical logins",
                user.getUsername(), loginEvents.size());

        CircularEwmaProfile profile = new CircularEwmaProfile(ALPHA);

        // Train profile on historical data (oldest to newest)
        for (Event event : loginEvents) {
            int hour = getHourFromEvent(event);
            profile.update(hour);
        }

        return profile;
    }

    /**
     * Extracts the hour (0-23) from an event timestamp.
     */
    private int getHourFromEvent(Event event) {
        return Instant.ofEpochMilli(event.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .getHour();
    }

    private void saveProfile(UserModel user, CircularEwmaProfile profile) {
        user.setAttribute(ATTR_MEAN_SIN, List.of(String.valueOf(profile.getMeanSin())));
        user.setAttribute(ATTR_MEAN_COS, List.of(String.valueOf(profile.getMeanCos())));

        logger.tracef("Saved time pattern for user %s: meanSin=%.4f, meanCos=%.4f",
                user.getUsername(), profile.getMeanSin(), profile.getMeanCos());
    }
}
