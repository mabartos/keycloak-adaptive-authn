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
import io.github.mabartos.context.user.LoginEventsContext;
import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.mabartos.level.Risk.Score.HIGH;
import static io.github.mabartos.level.Risk.Score.MEDIUM;
import static io.github.mabartos.level.Risk.Score.NONE;
import static io.github.mabartos.level.Risk.Score.VERY_HIGH;

/**
 * Risk evaluator for analyzing the distribution and pattern of failed logins over time
 * Detects suspicious patterns like credential stuffing, spray attacks, or systematic attempts
 */
public class FailedLoginPatternRiskEvaluator extends AbstractRiskEvaluator {
    private final LoginEventsContext loginEventsContext;
    private final List<EventType> RELEVANT_EVENTS = List.of(EventType.LOGIN, EventType.LOGIN_ERROR);

    public FailedLoginPatternRiskEvaluator(KeycloakSession session) {
        this.loginEventsContext = UserContexts.getContext(session, KcLoginEventsContextFactory.PROVIDER_ID);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.IMPORTANT;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var events = loginEventsContext.getData(realm, knownUser).orElse(null);
        if (events == null || events.size() < 3) {
            return Risk.invalid("Not enough login events");
        }

        long currentTime = Time.currentTimeMillis();

        // Analyze different time windows
        var analysis24h = analyzePattern(events, currentTime, Duration.ofHours(24).toMillis());
        var analysis1h = analyzePattern(events, currentTime, Duration.ofHours(1).toMillis());

        // Check for suspicious patterns
        Risk risk = Risk.of(NONE);

        // Pattern 1: High failure rate
        if (analysis1h.totalAttempts >= 5) {
            double failureRate = (double) analysis1h.failures / analysis1h.totalAttempts;
            if (failureRate >= 0.8 && analysis1h.failures >= 4) {
                risk = risk.max(Risk.of(VERY_HIGH,
                        String.format("%d failures out of %d attempts in 1h", analysis1h.failures, analysis1h.totalAttempts)));
            } else if (failureRate >= 0.6 && analysis1h.failures >= 3) {
                risk = risk.max(Risk.of(HIGH,
                        String.format("High failure rate in 1h: %.0f%%", failureRate * 100)));
            }
        }

        // Pattern 2: Systematic attempts from multiple IPs
        if (analysis1h.uniqueIps >= 3 && analysis1h.failures >= 3) {
            risk = risk.max(Risk.of(VERY_HIGH,
                    String.format("Distributed attack: %d IPs, %d failures in 1h", analysis1h.uniqueIps, analysis1h.failures)));
        } else if (analysis24h.uniqueIps >= 5 && analysis24h.failures >= 5) {
            risk = risk.max(Risk.of(HIGH,
                    String.format("Multiple IPs: %d IPs in 24h", analysis24h.uniqueIps)));
        }

        // Pattern 3: Regular intervals (bot-like behavior)
        if (analysis1h.failures >= 3) {
            var intervals = calculateTimingIntervals(events, currentTime - Duration.ofHours(1).toMillis());
            if (intervals.size() >= 2 && isRegularPattern(intervals)) {
                risk = risk.max(Risk.of(HIGH, "Regular timing pattern detected (bot-like)"));
            }
        }

        // Pattern 4: Burst of failures after period of inactivity
        if (analysis1h.failures >= 3 && analysis24h.totalAttempts == analysis1h.totalAttempts) {
            risk = risk.max(Risk.of(MEDIUM, "Sudden burst of activity"));
        }

        return risk;
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

    private boolean isRegularPattern(List<Long> intervals) {
        if (intervals.size() < 2) {
            return false;
        }

        // Calculate average interval
        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(0);

        // Check if intervals are within 30% of average (indicates regular pattern)
        double threshold = avg * 0.3;
        long regularCount = intervals.stream()
                .filter(interval -> Math.abs(interval - avg) <= threshold)
                .count();

        return regularCount >= intervals.size() * 0.7; // 70% of intervals are regular
    }

    private record PatternAnalysis(int totalAttempts, int failures, int successes, int uniqueIps) {
    }
}
