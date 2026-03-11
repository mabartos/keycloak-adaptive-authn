package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.context.user.TypicalAccessTimeContext;
import io.github.mabartos.context.user.TypicalAccessTimeContextFactory;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.ai.AiEngine;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects potential account takeover by analyzing complex behavioral patterns using AI.
 * <p>
 * <strong>Why AI Is Useful Here:</strong><br>
 * This evaluator detects subtle behavioral anomalies that suggest account compromise by analyzing
 * patterns across multiple dimensions simultaneously - something that's difficult to encode as
 * simple rules. AI can identify "this just doesn't feel right" scenarios.
 * <p>
 * <strong>What It Analyzes:</strong>
 * <ul>
 *   <li><strong>Behavioral Context:</strong> Sequence of recent actions (password resets, email changes,
 *       credential modifications) combined with login patterns</li>
 *   <li><strong>Temporal Patterns:</strong> Unusual timing of sensitive actions (e.g., password reset
 *       immediately followed by email change at 3am)</li>
 *   <li><strong>Geographic Anomalies:</strong> Location changes that don't make physical sense
 *       (e.g., login from US then 10 minutes later from China)</li>
 *   <li><strong>Device Fingerprint Changes:</strong> Sudden switches in browser/OS combined with
 *       other suspicious signals</li>
 *   <li><strong>Action Sequences:</strong> Patterns typical of attackers (e.g., change email →
 *       change password → delete MFA → access admin functions)</li>
 * </ul>
 * <p>
 * <strong>Real-World Example:</strong><br>
 * User normally logs in from Chrome/Windows in New York during business hours. Suddenly:
 * <pre>
 * 1. Login from Firefox/Linux in Romania at 2am
 * 2. Immediately attempts password reset
 * 3. Changes email to suspicious domain
 * 4. Tries to access admin panel
 * 5. Attempts to remove all MFA devices
 * </pre>
 * Each individual action might pass rule-based checks, but the combination and sequence
 * is highly suspicious. AI can detect this pattern.
 * <p>
 * <strong>When NOT to Use:</strong><br>
 * Disable this evaluator if:
 * - You don't have access to an AI service
 * - Latency is critical (AI adds 100-500ms)
 * - You want fully deterministic behavior
 * - Cost is a concern (AI API calls cost money)
 *
 * @see UserActionsRiskEvaluator for rule-based action counting
 */
public class AiAccountTakeoverEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiAccountTakeoverEvaluator.class);

    private final LoginEventsContext loginEvents;
    private final DeviceRepresentationContext deviceContext;
    private final TypicalAccessTimeContext typicalTimeContext;
    private final EventStoreProvider eventStore;
    private final AiEngine aiEngine;

    // Sensitive events that could indicate takeover
    private static final EventType[] TAKEOVER_INDICATORS = {
            EventType.UPDATE_EMAIL,
            EventType.UPDATE_EMAIL_ERROR,
            EventType.RESET_PASSWORD,
            EventType.RESET_PASSWORD_ERROR,
            EventType.UPDATE_CREDENTIAL,
            EventType.REMOVE_CREDENTIAL,
            EventType.DELETE_ACCOUNT,
            EventType.IMPERSONATE,
            EventType.GRANT_CONSENT,
            EventType.REVOKE_GRANT
    };

    public AiAccountTakeoverEvaluator(KeycloakSession session) {
        this.loginEvents = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContext.class);
        this.typicalTimeContext = UserContexts.getContext(session, TypicalAccessTimeContext.class);
        this.eventStore = session.getProvider(EventStoreProvider.class);
        this.aiEngine = session.getProvider(AiEngine.class);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.IMPORTANT;
    }

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

        if (aiEngine == null) {
            return Risk.invalid("AI engine not available");
        }

        // Gather behavioral context
        var behaviorContext = gatherBehavioralContext(realm, user);

        if (!behaviorContext.hasSufficientData()) {
            return Risk.invalid("Insufficient behavioral data for AI analysis");
        }

        // Send to AI for pattern analysis
        String prompt = buildPrompt(behaviorContext);
        logger.debugf("AI Account Takeover prompt for user %s: %s", user.getUsername(), prompt);

        return aiEngine.getRisk(prompt);
    }

    private BehavioralContext gatherBehavioralContext(RealmModel realm, UserModel user) {
        var context = new BehavioralContext();

        // Recent login history (last 24 hours)
        var recentLogins = loginEvents.getData(realm, user)
                .map(events -> events.stream()
                        .filter(e -> e.getType() == EventType.LOGIN)
                        .filter(e -> e.getTime() > Time.currentTimeMillis() - Duration.ofHours(24).toMillis())
                        .toList())
                .orElse(List.of());

        context.recentLoginCount = recentLogins.size();
        context.recentLoginIps = recentLogins.stream()
                .map(Event::getIpAddress)
                .distinct()
                .collect(Collectors.toList());

        // Current device info
        deviceContext.getData(realm, user).ifPresent(device -> {
            context.currentDevice = String.format("%s on %s %s",
                    device.getBrowser(), device.getOs(), device.getOsVersion());
            context.currentIp = device.getIpAddress();
            context.isMobile = device.isMobile();
        });

        // Load typical login time from shared context
        var timeData = typicalTimeContext.getData(realm, user);
        if (timeData.isPresent() && timeData.get().hasSufficientData()) {
            context.typicalLoginHour = timeData.get().getTypicalLoginHour();
        } else {
            // Not enough data yet - will be null in prompt
            context.typicalLoginHour = null;
        }

        // Recent sensitive actions (last 30 minutes)
        var recentActions = eventStore.createQuery()
                .realm(realm.getId())
                .user(user.getId())
                .type(TAKEOVER_INDICATORS)
                .fromDate(new Date(Time.currentTimeMillis() - Duration.ofMinutes(30).toMillis()))
                .getResultStream()
                .toList();

        context.recentSensitiveActions = recentActions.stream()
                .map(this::formatEventForAi)
                .collect(Collectors.toList());

        // Check for rapid location changes
        if (recentLogins.size() >= 2) {
            var ips = recentLogins.stream()
                    .map(Event::getIpAddress)
                    .distinct()
                    .toList();

            if (ips.size() > 1) {
                context.hasMultipleRecentLocations = true;
                context.recentLocationCount = ips.size();
            }
        }

        return context;
    }

    private String formatEventForAi(Event event) {
        var time = Instant.ofEpochMilli(event.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return String.format("%s at %s from IP %s",
                event.getType().toString().replace("_", " ").toLowerCase(),
                time.toString(),
                event.getIpAddress());
    }

    private String buildPrompt(BehavioralContext ctx) {
        String typicalTimeStr = ctx.typicalLoginHour != null ?
                String.format("~%02d:00", ctx.typicalLoginHour) :
                "unknown (insufficient data)";

        return String.format("""
                Analyze this user session for potential account takeover. Look for suspicious behavioral patterns.

                CURRENT SESSION:
                - Device: %s
                - IP: %s
                - Is Mobile: %s

                NORMAL BEHAVIOR BASELINE:
                - Typical login time: %s
                - Recent logins (24h): %d
                - Recent IPs: %s
                - Multiple recent locations: %s

                RECENT SENSITIVE ACTIONS (last 30 min):
                %s

                ANALYZE FOR:
                1. Impossible travel (location changes too fast)
                2. Suspicious action sequences (e.g., change email → change password → remove MFA)
                3. Unusual timing (sensitive actions at unusual hours)
                4. Device fingerprint changes combined with suspicious actions
                5. Rapid succession of privilege escalation attempts

                Consider the COMBINATION of factors. Individual changes are normal, but multiple red flags together indicate takeover.
                """,
                ctx.currentDevice,
                ctx.currentIp,
                ctx.isMobile,
                typicalTimeStr,
                ctx.recentLoginCount,
                ctx.recentLoginIps.isEmpty() ? "none" : String.join(", ", ctx.recentLoginIps),
                ctx.hasMultipleRecentLocations ?
                    String.format("YES (%d different locations)", ctx.recentLocationCount) : "NO",
                ctx.recentSensitiveActions.isEmpty() ?
                    "None" : String.join("\n", ctx.recentSensitiveActions)
        );
    }

    private static class BehavioralContext {
        String currentDevice = "unknown";
        String currentIp = "unknown";
        boolean isMobile = false;
        Integer typicalLoginHour = null; // Nullable - may not have enough data yet
        int recentLoginCount = 0;
        List<String> recentLoginIps = List.of();
        boolean hasMultipleRecentLocations = false;
        int recentLocationCount = 0;
        List<String> recentSensitiveActions = List.of();

        boolean hasSufficientData() {
            // Need at least current device info to make an assessment
            return !"unknown".equals(currentDevice) && !"unknown".equals(currentIp);
        }
    }
}
