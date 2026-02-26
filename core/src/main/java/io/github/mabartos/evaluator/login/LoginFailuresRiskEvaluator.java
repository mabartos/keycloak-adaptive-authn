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

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.ip.client.DefaultIpAddressContextFactory;
import io.github.mabartos.context.ip.client.IpAddressContext;
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
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Risk evaluator for checking login failures properties to detect brute-force attacks
 */
public class LoginFailuresRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(LoginFailuresRiskEvaluator.class);

    private final KeycloakSession session;
    private final IpAddressContext ipAddressContext;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.ipAddressContext = UserContexts.getContext(session, DefaultIpAddressContextFactory.PROVIDER_ID);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.IMPORTANT;
    }

    protected double getRiskLoginFailures(int failuresCount) {
        if (failuresCount <= 2) {
            return Risk.NONE;
        } else if (failuresCount <= 5) {
            return Risk.SMALL;
        } else if (failuresCount < 10) {
            return Risk.MEDIUM;
        } else if (failuresCount < 15) {
            return Risk.INTERMEDIATE;
        } else {
            return Risk.VERY_HIGH;
        }
    }

    protected double getRiskLastFailureTime(long lastFailureTime) {
        if (lastFailureTime == 0) {
            return Risk.NONE;
        }

        long timeSinceLastFailure = Time.currentTimeMillis() - lastFailureTime;

        // Recent failures are more concerning
        if (timeSinceLastFailure < Duration.ofMinutes(5).toMillis()) {
            return Risk.VERY_HIGH;
        } else if (timeSinceLastFailure < Duration.ofMinutes(15).toMillis()) {
            return Risk.INTERMEDIATE;
        } else if (timeSinceLastFailure < Duration.ofHours(1).toMillis()) {
            return Risk.MEDIUM;
        } else if (timeSinceLastFailure < Duration.ofHours(24).toMillis()) {
            return Risk.SMALL;
        } else {
            return Risk.NONE;
        }
    }

    protected Optional<Double> getRiskLastIP(RealmModel realmModel, UserModel knownUser, String lastIP) {
        var currentIp = ipAddressContext.getData(realmModel, knownUser).map(IPAddress::toString).orElse("");

        if (StringUtil.isBlank(currentIp) || StringUtil.isBlank(lastIP)) {
            logger.trace("Missing IP address information");
            return Optional.empty();
        }

        if (!currentIp.equals(lastIP)) {
            logger.trace("Request from different IP address");
            return Optional.of(Risk.INTERMEDIATE);
        } else {
            logger.trace("Same IP address");
        }

        return Optional.empty();
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm, @Nullable UserModel knownUser) {
        if (knownUser == null) {
            return Risk.invalid("User is null");
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, knownUser.getId());
        if (loginFailures == null) {
            return Risk.invalid("Cannot obtain login failures");
        }

        // Number of failures
        var numFailures = loginFailures.getNumFailures();
        double failuresRisk = getRiskLoginFailures(numFailures);

        // Time since last failure
        double timeRisk = getRiskLastFailureTime(loginFailures.getLastFailure());

        // IP address check
        double ipRisk = getRiskLastIP(realm, knownUser, loginFailures.getLastIPFailure()).orElse(Risk.NONE);

        // Take the maximum risk among all factors
        double maxRisk = Math.max(Math.max(failuresRisk, timeRisk), ipRisk);

        return Risk.of(maxRisk);
    }
}
