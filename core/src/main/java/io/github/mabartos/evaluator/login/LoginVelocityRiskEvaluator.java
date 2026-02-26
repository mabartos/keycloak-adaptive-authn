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
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Risk evaluator for detecting rapid successive login attempts
 * that could indicate credential stuffing or brute force attacks
 */
public class LoginVelocityRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(LoginVelocityRiskEvaluator.class);

    private final LoginEventsContext loginEventsContext;

    public LoginVelocityRiskEvaluator(KeycloakSession session) {
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

        var eventsOptional = loginEventsContext.getData(realm, knownUser);
        if (eventsOptional.isEmpty()) {
            return Risk.notEnoughInfo("No login events");
        }

        var events = eventsOptional.get();
        if (events.size() < 2) {
            return Risk.notEnoughInfo("Not enough login events");
        }

        long currentTime = Time.currentTimeMillis();

        // Count login attempts in different time windows
        long oneMinuteAgo = currentTime - Duration.ofMinutes(1).toMillis();
        long fiveMinutesAgo = currentTime - Duration.ofMinutes(5).toMillis();
        long fifteenMinutesAgo = currentTime - Duration.ofMinutes(15).toMillis();

        long attemptsInOneMinute = countAttemptsSince(events, oneMinuteAgo);
        long attemptsInFiveMinutes = countAttemptsSince(events, fiveMinutesAgo);
        long attemptsInFifteenMinutes = countAttemptsSince(events, fifteenMinutesAgo);

        // Very rapid attempts in the last minute
        if (attemptsInOneMinute >= 3) {
            return Risk.of(Risk.HIGHEST, "3+ login attempts in 1 minute");
        } else if (attemptsInOneMinute >= 2) {
            return Risk.of(Risk.VERY_HIGH, "2 login attempts in 1 minute");
        }

        // Multiple attempts in 5 minutes
        if (attemptsInFiveMinutes >= 5) {
            return Risk.of(Risk.VERY_HIGH, "5+ login attempts in 5 minutes");
        } else if (attemptsInFiveMinutes >= 3) {
            return Risk.of(Risk.INTERMEDIATE, "3+ login attempts in 5 minutes");
        }

        // Many attempts in 15 minutes
        if (attemptsInFifteenMinutes >= 8) {
            return Risk.of(Risk.INTERMEDIATE, "8+ login attempts in 15 minutes");
        } else if (attemptsInFifteenMinutes >= 5) {
            return Risk.of(Risk.MEDIUM, "5+ login attempts in 15 minutes");
        }

        return Risk.none();
    }

    private long countAttemptsSince(List<org.keycloak.events.Event> events, long sinceTime) {
        return events.stream()
                .filter(event -> event.getType() == EventType.LOGIN || event.getType() == EventType.LOGIN_ERROR)
                .filter(event -> event.getTime() >= sinceTime)
                .count();
    }
}
