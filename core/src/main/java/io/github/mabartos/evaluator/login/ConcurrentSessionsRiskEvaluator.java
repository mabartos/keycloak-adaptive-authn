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
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.account.DeviceRepresentation;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Risk evaluator for detecting suspicious concurrent active sessions
 * from different locations or devices
 */
public class ConcurrentSessionsRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(ConcurrentSessionsRiskEvaluator.class);

    // Time window to consider a session as "active"
    private static final long ACTIVE_SESSION_THRESHOLD_MS = Duration.ofMinutes(30).toMillis();

    private final KeycloakSession session;
    private final DeviceRepresentationContext deviceContext;

    public ConcurrentSessionsRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContextFactory.PROVIDER_ID);
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

        var currentDevice = deviceContext.getData(realm, knownUser).orElse(null);
        if (currentDevice == null) {
            logger.tracef("No current device information");
            return Risk.invalid("No device information");
        }

        // Get all active user sessions
        List<UserSessionModel> userSessions = getActiveUserSessions(realm, knownUser);

        if (userSessions.isEmpty()) {
            return Risk.notEnoughInfo("No active sessions");
        }

        if (userSessions.size() == 1) {
            return Risk.none();
        }

        // Analyze concurrent sessions
        Set<String> uniqueIpAddresses = new HashSet<>();
        Set<String> uniqueBrowsers = new HashSet<>();
        Set<String> uniqueOS = new HashSet<>();
        long currentTime = Time.currentTimeMillis();

        for (UserSessionModel userSession : userSessions) {
            String ipAddress = userSession.getIpAddress();
            if (ipAddress != null) {
                uniqueIpAddresses.add(ipAddress);
            }

            String browser = userSession.getNote("browser");
            if (browser != null) {
                uniqueBrowsers.add(browser);
            }

            String os = userSession.getNote("os");
            if (os != null) {
                uniqueOS.add(os);
            }
        }

        // Calculate risk based on session diversity
        double risk = Risk.NONE;
        String reason = "";

        // Pattern 1: Multiple sessions from different IPs
        if (uniqueIpAddresses.size() >= 3) {
            risk = Math.max(risk, Risk.VERY_HIGH);
            reason = String.format("%d concurrent sessions from %d different IPs",
                    userSessions.size(), uniqueIpAddresses.size());
        } else if (uniqueIpAddresses.size() >= 2) {
            // Check if current IP is new
            String currentIp = currentDevice.getIpAddress();
            if (currentIp != null && !uniqueIpAddresses.contains(currentIp)) {
                risk = Math.max(risk, Risk.INTERMEDIATE);
                reason = String.format("New IP with %d existing sessions", userSessions.size());
            } else {
                risk = Math.max(risk, Risk.SMALL);
                reason = String.format("%d sessions from %d IPs",
                        userSessions.size(), uniqueIpAddresses.size());
            }
        }

        // Pattern 2: Many concurrent sessions (account sharing or compromise)
        if (userSessions.size() >= 5) {
            risk = Math.max(risk, Risk.INTERMEDIATE);
            reason = String.format("%d concurrent active sessions", userSessions.size());
        } else if (userSessions.size() >= 3) {
            risk = Math.max(risk, Risk.SMALL);
            reason = String.format("%d concurrent sessions", userSessions.size());
        }

        // Pattern 3: Different operating systems or browsers (less critical)
        if (uniqueOS.size() >= 3 || uniqueBrowsers.size() >= 3) {
            risk = Math.max(risk, Risk.SMALL);
            reason = String.format("Sessions from %d different OS/%d browsers",
                    uniqueOS.size(), uniqueBrowsers.size());
        }

        if (risk > Risk.NONE) {
            logger.tracef("Concurrent sessions risk: %s", reason);
            return Risk.of(risk, reason);
        }

        return Risk.none();
    }

    private List<UserSessionModel> getActiveUserSessions(RealmModel realm, UserModel user) {
        long currentTime = Time.currentTimeMillis();
        long activeThreshold = currentTime - ACTIVE_SESSION_THRESHOLD_MS;

        // Get user sessions from all available session providers
        Stream<UserSessionModel> offlineSessions = session.sessions()
                .getOfflineUserSessionsStream(realm, user);

        Stream<UserSessionModel> onlineSessions = session.sessions()
                .getUserSessionsStream(realm, user);

        return Stream.concat(offlineSessions, onlineSessions)
                .filter(s -> {
                    // Consider session active if it was used recently
                    long lastSessionRefresh = s.getLastSessionRefresh();
                    return lastSessionRefresh > 0 && lastSessionRefresh >= (activeThreshold / 1000);
                })
                .collect(Collectors.toList());
    }
}
