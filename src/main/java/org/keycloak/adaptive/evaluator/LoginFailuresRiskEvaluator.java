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
package org.keycloak.adaptive.evaluator;

import inet.ipaddr.IPAddress;
import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.ip.client.DefaultIpAddressFactory;
import org.keycloak.adaptive.context.ip.client.IpAddressContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

public class LoginFailuresRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(LoginFailuresRiskEvaluator.class);

    private final KeycloakSession session;
    private final IpAddressContext ipAddressContext;
    private Double risk;

    public LoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.ipAddressContext = ContextUtils.getContext(session, DefaultIpAddressFactory.PROVIDER_ID);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(
                session,
                LoginFailuresRiskEvaluatorFactory.class,
                Weight.IMPORTANT
        );
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(
                session,
                LoginFailuresRiskEvaluatorFactory.class
        );
    }

    @Override
    public void evaluate() {
        var realm = session.getContext().getRealm();
        if (realm == null) {
            logger.debug("Context realm is null");
            return;
        }

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) {
            logger.debug("Context user is null");
            return;
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) {
            logger.debug("Cannot obtain login failures");
            return;
        }

        // Number of failures
        var numFailures = loginFailures.getNumFailures();
        if (numFailures <= 2) {
            this.risk = Risk.NONE;
        } else if (numFailures <= 5) {
            this.risk = Risk.SMALL;
        } else if (numFailures < 10) {
            this.risk = Risk.MEDIUM;
        } else if (numFailures < 15) {
            this.risk = Risk.INTERMEDIATE;
        } else {
            this.risk = Risk.VERY_HIGH;
        }

        var currentIp = Optional.ofNullable(ipAddressContext.getData()).map(IPAddress::toString).orElse("");
        var lastIpFailure = loginFailures.getLastIPFailure();

        if (StringUtil.isBlank(currentIp) || StringUtil.isBlank(lastIpFailure)) {
            if (!currentIp.equals(lastIpFailure)) {
                this.risk = Math.max(risk, Risk.INTERMEDIATE);
                logger.debug("Request from different IP address");
            }
        }

        // TODO compute when was the last login failure
    }
}
