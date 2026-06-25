/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mabartos.evaluator.login;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.user.KcLoginEventsContextFactory;
import io.github.mabartos.context.user.KcLoginFailuresEventsContextFactory;
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;

/**
 * Detects attack-signature patterns in failed logins: distributed multi-IP attacks and bot-like timing regularity.
 * Raw failure counts and recency are handled by {@link LoginFailuresRiskEvaluator}.
 */
@EvaluationPhase(USER_KNOWN)
public class FailedLoginPatternRiskEvaluator extends AbstractRiskEvaluator {
    private final LoginEventsContext loginEventsContext;
    private final LoginEventsContext loginFailuresEventsContext;
    private final List<EventType> RELEVANT_EVENTS = List.of(EventType.LOGIN, EventType.LOGIN_ERROR);

    public FailedLoginPatternRiskEvaluator(KeycloakSession session) {
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
        this.loginFailuresEventsContext = UserContexts.getContext(session, KcLoginFailuresEventsContextFactory.PROVIDER_ID);
    }

    FailedLoginPatternRiskEvaluator(LoginEventsContext loginEventsContext, LoginEventsContext loginFailuresEventsContext) {
        this.loginEventsContext = loginEventsContext;
        this.loginFailuresEventsContext = loginFailuresEventsContext;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var events = loadLoginActivityEvents(realm, knownUser);
        if (events.size() < 3) {
            return Risk.invalid("Not enough login events");
        }

        long currentTime = Time.currentTimeMillis();

        // Analyze different time windows
        var analysis24h = analyzePattern(events, currentTime, Duration.ofHours(24).toMillis());
        var analysis1h = analyzePattern(events, currentTime, Duration.ofHours(1).toMillis());

        Risk risk = Risk.of(NONE);

        // Distributed attack from multiple IPs
        if (analysis1h.uniqueIps >= 5 && analysis1h.failures >= 5) {
            risk = risk.max(Risk.of(HIGH,
                    String.format("Distributed attack: %d IPs, %d failures in 1h", analysis1h.uniqueIps, analysis1h.failures)));
        } else if (analysis1h.uniqueIps >= 3 && analysis1h.failures >= 3) {
            risk = risk.max(Risk.of(MEDIUM,
                    String.format("Multiple source IPs: %d IPs, %d failures in 1h", analysis1h.uniqueIps, analysis1h.failures)));
        } else if (analysis24h.uniqueIps >= 5 && analysis24h.failures >= 5) {
            risk = risk.max(Risk.of(SMALL,
                    String.format("Multiple IPs over 24h: %d IPs", analysis24h.uniqueIps)));
        }

        // Regular intervals (bot-like behavior)
        if (analysis1h.failures >= 4) {
            var intervals = calculateTimingIntervals(events, currentTime - Duration.ofHours(1).toMillis());
            if (intervals.size() >= 3 && isRegularPattern(intervals)) {
                risk = risk.max(Risk.of(MEDIUM, "Regular timing pattern detected (bot-like)"));
            }
        }

        return risk;
    }

    private List<Event> loadLoginActivityEvents(RealmModel realm, UserModel knownUser) {
        List<Event> events = new ArrayList<>();
        loginEventsContext.getData(realm, knownUser).ifPresent(events::addAll);
        loginFailuresEventsContext.getData(realm, knownUser).ifPresent(events::addAll);
        return events;
    }

    private PatternAnalysis analyzePattern(List<Event> events, long currentTime, long timeWindowMs) {
        long windowStart = currentTime - timeWindowMs;

        var relevantEvents = events.stream()
                .filter(e -> e.getTime() >= windowStart)
                .filter(e -> RELEVANT_EVENTS.contains(e.getType()))
                .toList();

        int failures = (int) relevantEvents.stream()
                .filter(e -> e.getType() == EventType.LOGIN_ERROR)
                .count();

        int successes = (int) relevantEvents.stream()
                .filter(e -> e.getType() == EventType.LOGIN)
                .count();

        Set<String> uniqueIps = relevantEvents.stream()
                .map(Event::getIpAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new PatternAnalysis(
                relevantEvents.size(),
                failures,
                successes,
                uniqueIps.size()
        );
    }

    private List<Long> calculateTimingIntervals(List<Event> events, long sinceTime) {
        var sortedEvents = events.stream()
                .filter(e -> e.getTime() >= sinceTime)
                .filter(e -> e.getType() == EventType.LOGIN_ERROR)
                .sorted(Comparator.comparingLong(Event::getTime))
                .toList();

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sortedEvents.size(); i++) {
            intervals.add(sortedEvents.get(i).getTime() - sortedEvents.get(i - 1).getTime());
        }

        return intervals;
    }

    private static final long MAX_ABSOLUTE_DEVIATION_MS = 50;

    private boolean isRegularPattern(List<Long> intervals) {
        if (intervals.size() < 2) {
            return false;
        }

        // Check if intervals deviate by at most 50ms from the average (machine-level precision).
        // Bots produce near-identical intervals regardless of speed; humans deviate by hundreds of ms.
        //
        // Fast bot (100ms intervals):  deviations ~10-40ms → 40 <= 50 → bot pattern
        // Slow bot (5000ms intervals): deviations ~10-15ms → 15 <= 50 → bot pattern
        // Human (3000ms intervals):    deviations ~200-700ms → 700 > 50 → not a bot pattern
        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(0);

        long regularCount = intervals.stream()
                .filter(interval -> Math.abs(interval - avg) <= MAX_ABSOLUTE_DEVIATION_MS)
                .count();

        return regularCount >= intervals.size() * 0.7; // 70% of intervals must match
    }

    private record PatternAnalysis(int totalAttempts, int failures, int successes, int uniqueIps) {
    }
}
