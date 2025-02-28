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
package org.keycloak.adaptive.evaluator.login;

import inet.ipaddr.IPAddress;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.ip.client.DefaultIpAddressFactory;
import org.keycloak.adaptive.context.ip.client.IpAddressContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

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
        this.ipAddressContext = UserContexts.getContext(session, DefaultIpAddressFactory.PROVIDER_ID);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
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

    protected Optional<Double> getRiskLastIP(String lastIP) {
        var currentIp = ipAddressContext.getData().map(IPAddress::toString).orElse("");

        if (StringUtil.isBlank(currentIp) || StringUtil.isBlank(lastIP)) {
            if (!currentIp.equals(lastIP)) {
                logger.trace("Request from different IP address");
                return Optional.of(Risk.INTERMEDIATE);
            } else {
                logger.trace("Same IP address");
            }
        }
        return Optional.empty();
    }

    @Override
    public Risk evaluate() {
        var realm = session.getContext().getRealm();
        if (realm == null) {
            logger.trace("Context realm is null");
            return Risk.invalid();
        }

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) {
            logger.trace("Context user is null");
            return Risk.invalid();
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) {
            logger.trace("Cannot obtain login failures");
            return Risk.invalid();
        }

        // Number of failures
        var numFailures = loginFailures.getNumFailures();

        return getRiskLastIP(loginFailures.getLastIPFailure())
                .map(score -> Math.max(score, getRiskLoginFailures(numFailures)))
                .map(Risk::of)
                .orElseGet(() -> Risk.of(getRiskLoginFailures(numFailures)));

        // TODO compute when was the last login failure
    }
}
