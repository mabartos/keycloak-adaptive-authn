package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.ContinuousRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.CONTINUOUS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;

/**
 * Detects suspicious concurrent sessions for the same user by analyzing two independent signals:
 * <p>
 * <strong>Signal 1: Distinct IP count</strong><br>
 * Multiple IPs are normal — laptop on WiFi and phone on cellular have different IPs.
 * But a very high number of distinct IPs indicates credential compromise or sharing.
 * <pre>
 * 1 session, 1-2 IPs  = NEGATIVE_LOW (clean usage — positive evidence)
 * 2-3 sessions, 1-2 IPs = NEGATIVE_LOW (normal multi-device — laptop + phone)
 * 2-3 sessions, 3 IPs   = NONE        (neutral, plausible but no positive signal)
 * 4   distinct IPs       = SMALL
 * 5-6 distinct IPs       = MEDIUM
 * 7-9 distinct IPs       = HIGH
 * 10+ distinct IPs       = VERY_HIGH
 * </pre>
 * <p>
 * <strong>Signal 2: Max sessions per single IP</strong><br>
 * Behind NAT, many bots or compromised machines share the same public IP.
 * A legitimate user might have 2-3 sessions from the same IP (browser, app, another client),
 * but a high count from one IP suggests automation or credential sharing within the same network.
 * <pre>
 * 1 session/IP   = NEGATIVE_LOW (no clustering — healthy)
 * 2-3 sessions/IP = NONE
 * 4-5 sessions/IP = SMALL
 * 6-8 sessions/IP = MEDIUM
 * 9-12 sessions/IP = HIGH
 * 13+ sessions/IP  = VERY_HIGH
 * </pre>
 * <p>
 * The final risk is the maximum of both signals.
 */
@EvaluationPhase(CONTINUOUS)
public class ConcurrentSessionRiskEvaluator extends ContinuousRiskEvaluator {
    private static final Logger logger = Logger.getLogger(ConcurrentSessionRiskEvaluator.class);

    private final KeycloakSession session;

    public ConcurrentSessionRiskEvaluator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel user) {
        if (user == null) {
            return Risk.invalid("Cannot find user");
        }

        var userSessions = session.sessions()
                .getUserSessionsStream(realm, user)
                .toList();

        int sessionCount = userSessions.size();

        if (sessionCount == 0) {
            return Risk.of(NONE);
        }

        // Single session from a single IP — clean usage
        if (sessionCount == 1) {
            return Risk.of(NEGATIVE_LOW, "Single active session — clean usage");
        }

        // Group sessions by IP
        Map<String, Long> sessionsByIp = userSessions.stream()
                .map(UserSessionModel::getIpAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ip -> ip, Collectors.counting()));

        int distinctIpCount = sessionsByIp.size();
        long maxSessionsPerIp = sessionsByIp.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        // Signal 1: Distinct IP count
        Risk ipRisk = evaluateDistinctIps(distinctIpCount, sessionCount);

        // Signal 2: Max sessions from a single IP (NAT-hidden bots/sharing)
        Risk perIpRisk = evaluateSessionsPerIp(maxSessionsPerIp, sessionCount);

        Risk risk = ipRisk.max(perIpRisk);

        logger.debugf("User %s has %d active sessions from %d distinct IPs (max %d sessions/IP). Risk: %s",
                user.getId(), sessionCount, distinctIpCount, maxSessionsPerIp, risk.getScore());

        return risk;
    }

    private Risk evaluateDistinctIps(int distinctIpCount, int sessionCount) {
        if (distinctIpCount <= 2) {
            return Risk.of(NEGATIVE_LOW,
                    String.format("%d sessions from %d IP(s) — normal multi-device usage", sessionCount, distinctIpCount));
        } else if (distinctIpCount == 3) {
            return Risk.of(NONE,
                    String.format("%d sessions from 3 IPs", sessionCount));
        } else if (distinctIpCount == 4) {
            return Risk.of(SMALL,
                    String.format("%d sessions from 4 distinct IPs", sessionCount));
        } else if (distinctIpCount <= 6) {
            return Risk.of(MEDIUM,
                    String.format("%d sessions from %d distinct IPs", sessionCount, distinctIpCount));
        } else if (distinctIpCount <= 9) {
            return Risk.of(HIGH,
                    String.format("%d sessions from %d distinct IPs", sessionCount, distinctIpCount));
        } else {
            return Risk.of(VERY_HIGH,
                    String.format("%d sessions from %d distinct IPs - possible credential compromise",
                            sessionCount, distinctIpCount));
        }
    }

    private Risk evaluateSessionsPerIp(long maxSessionsPerIp, int sessionCount) {
        if (maxSessionsPerIp <= 1) {
            return Risk.of(NEGATIVE_LOW,
                    String.format("Max 1 session per IP (%d total) — no clustering", sessionCount));
        } else if (maxSessionsPerIp <= 3) {
            return Risk.of(NONE,
                    String.format("%d max sessions from single IP (%d total)", maxSessionsPerIp, sessionCount));
        } else if (maxSessionsPerIp <= 5) {
            return Risk.of(SMALL,
                    String.format("%d max sessions from single IP (%d total)", maxSessionsPerIp, sessionCount));
        } else if (maxSessionsPerIp <= 8) {
            return Risk.of(MEDIUM,
                    String.format("%d max sessions from single IP (%d total)", maxSessionsPerIp, sessionCount));
        } else if (maxSessionsPerIp <= 12) {
            return Risk.of(HIGH,
                    String.format("%d max sessions from single IP (%d total)", maxSessionsPerIp, sessionCount));
        } else {
            return Risk.of(VERY_HIGH,
                    String.format("%d max sessions from single IP - possible automation (%d total)",
                            maxSessionsPerIp, sessionCount));
        }
    }
}
