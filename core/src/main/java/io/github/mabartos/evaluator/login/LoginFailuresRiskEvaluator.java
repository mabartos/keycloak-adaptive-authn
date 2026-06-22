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

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.EvaluationPhase;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;

import static io.github.mabartos.spi.evaluator.RiskEvaluator.EvaluationPhase.USER_KNOWN;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.common.util.Time;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;

/**
 * Risk evaluator for checking login failures to detect brute-force attacks.
 * Uses LOGIN_ERROR events from the event store instead of the brute force subsystem.
 */
@EvaluationPhase(USER_KNOWN)
public class LoginFailuresRiskEvaluator extends AbstractRiskEvaluator {
    private static final int MAX_EVENTS = 90;
    private static final int MAX_SUCCESSFUL_LOGINS = 30;
    private static final Duration RECENT_WINDOW = Duration.ofMinutes(1);

    private final EventStoreProvider eventStore;
    private final IpAddressContext ipAddressContext;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.eventStore = session.getProvider(EventStoreProvider.class);
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    protected Risk getRiskLoginFailures(int failuresCount) {
        if (failuresCount <= 3) {
            return Risk.of(NONE);
        } else if (failuresCount <= 7) {
            return Risk.of(SMALL, failuresCount + " login failures recorded");
        } else if (failuresCount < 15) {
            return Risk.of(MEDIUM, failuresCount + " login failures recorded");
        } else if (failuresCount < 25) {
            return Risk.of(HIGH, failuresCount + " login failures recorded");
        } else {
            return Risk.of(VERY_HIGH, failuresCount + " login failures recorded - possible brute-force attack");
        }
    }

    protected Risk getRiskLastFailureTime(long lastFailureTime) {
        if (lastFailureTime == 0) {
            return Risk.of(NONE);
        }

        long timeSinceLastFailure = Time.currentTimeMillis() - lastFailureTime;

        // Recent failures are more concerning
        if (timeSinceLastFailure < Duration.ofMinutes(5).toMillis()) {
            return Risk.of(MEDIUM);
        } else if (timeSinceLastFailure < Duration.ofMinutes(15).toMillis()) {
            return Risk.of(SMALL);
        } else {
            return Risk.of(NONE);
        }
    }

    protected Risk getRiskLastIP(RealmModel realmModel, UserModel knownUser, String lastIP) {
        var currentIp = ipAddressContext.getData(realmModel, knownUser).map(IPAddress::toString).orElse("");

        if (StringUtil.isBlank(currentIp) || StringUtil.isBlank(lastIP)) {
            return Risk.invalid("Not enough information about IP addresses");
        }

        if (!currentIp.equals(lastIP)) {
            return Risk.of(MEDIUM, "Request from different IP address");
        }

        return Risk.of(NONE);
    }

    /**
     * If the current IP matches a previously successful login IP, reduce the risk.
     * The legitimate user should not be blocked by an attacker's failed attempts.
     */
    protected Risk mitigateWithKnownIp(Risk currentRisk, RealmModel realm, UserModel knownUser) {
        var currentIp = ipAddressContext.getData(realm, knownUser).map(IPAddress::toString).orElse("");
        if (StringUtil.isBlank(currentIp)) {
            return currentRisk;
        }

        var knownSuccessfulIps = eventStore.createQuery()
                .realm(realm.getId())
                .user(knownUser.getId())
                .type(EventType.LOGIN)
                .maxResults(MAX_SUCCESSFUL_LOGINS)
                .orderByDescTime()
                .getResultStream()
                .map(Event::getIpAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (knownSuccessfulIps.contains(currentIp)) {
            return Risk.of(SMALL, currentRisk.getReason().orElse("")
                    + " (mitigated: current IP matches a known successful login)");
        }

        return currentRisk;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var failureEvents = eventStore.createQuery()
                .realm(realm.getId())
                .user(knownUser.getId())
                .type(EventType.LOGIN_ERROR)
                .maxResults(MAX_EVENTS)
                .orderByDescTime()
                .getResultStream()
                .toList();

        if (failureEvents.isEmpty()) {
            return Risk.of(NEGATIVE_LOW, "No login failures recorded - trust signal");
        }

        Event lastFailure = failureEvents.getFirst();

        // Count failures within the recent window
        long recentWindowStart = Time.currentTimeMillis() - RECENT_WINDOW.toMillis();
        int recentFailures = (int) failureEvents.stream()
                .filter(e -> e.getTime() >= recentWindowStart)
                .count();

        var resultRisk = Risk.of(NONE);

        // Number of recent failures
        resultRisk = resultRisk.max(getRiskLoginFailures(recentFailures));

        // Time since last failure
        resultRisk = resultRisk.max(getRiskLastFailureTime(lastFailure.getTime()));

        // IP address check
        resultRisk = resultRisk.max(getRiskLastIP(realm, knownUser, lastFailure.getIpAddress()));

        // If risk is HIGH or VERY_HIGH, check if the current IP is a known successful login IP.
        // This prevents DoS attacks where an attacker floods failures to block a legitimate user.
        if (resultRisk.getScore() == HIGH || resultRisk.getScore() == VERY_HIGH) {
            resultRisk = mitigateWithKnownIp(resultRisk, realm, knownUser);
        }

        return resultRisk;
    }
}
