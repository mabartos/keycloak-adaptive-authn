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
import io.github.mabartos.context.ip.client.IpAddressContext;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.level.Trust;
import io.github.mabartos.spi.evaluator.AbstractRiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.time.Duration;
import java.util.Set;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;

/**
 * Risk evaluator for checking login failures properties to detect brute-force attacks
 */
public class LoginFailuresRiskEvaluator extends AbstractRiskEvaluator {
    private final KeycloakSession session;
    private final IpAddressContext ipAddressContext;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.ipAddressContext = UserContexts.getContext(session, IpAddressContext.class);
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.USER_KNOWN);
    }

    protected Risk getRiskLoginFailures(int failuresCount) {
        if (failuresCount <= 3) {
            return Risk.of(NONE);
        } else if (failuresCount <= 7) {
            return Risk.of(SMALL);
        } else if (failuresCount < 15) {
            return Risk.of(MEDIUM);
        } else if (failuresCount < 25) {
            return Risk.of(HIGH);
        } else {
            return Risk.of(VERY_HIGH);
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
            return Risk.of(HIGH, "Request from different IP address");
        }

        return Risk.of(NONE);
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

        var resultRisk = Risk.invalid("No login failures evaluation happened");

        // Number of failures
        resultRisk = resultRisk.max(getRiskLoginFailures(loginFailures.getNumFailures()));

        // Time since last failure
        resultRisk = resultRisk.max(getRiskLastFailureTime(loginFailures.getLastFailure()));

        // IP address check
        resultRisk = resultRisk.max(getRiskLastIP(realm, knownUser, loginFailures.getLastIPFailure()));

        return resultRisk;
    }
}
