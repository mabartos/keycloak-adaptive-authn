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
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.ip.client.DefaultIpAddressFactory;
import org.keycloak.adaptive.context.ip.client.IpAddressContext;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

/**
 * Risk evaluator for checking login failures properties evaluated by AI NLP engine to detect brute-force attacks
 */
public class AiLoginFailuresRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiLoginFailuresRiskEvaluator.class);

    private final KeycloakSession session;
    private final IpAddressContext ipAddressContext;
    private final AiNlpEngine aiEngine;

    public AiLoginFailuresRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.ipAddressContext = ContextUtils.getContext(session, DefaultIpAddressFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiNlpEngine.class);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public double getDefaultWeight() {
        return Weight.IMPORTANT;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    protected String request(UserLoginFailureModel loginFailures) {
        // we should be careful about the message poisoning
        var request = String.format("""
                        Give me the overall risk that the user trying to authenticate is a fraud based on its parameters.
                        These parameters show the metrics about login failures for the particular user.
                        Used for detection of brute force attacks.
                        After each successful login, these metrics are reset.
                        -----
                        Number of login failures for the user: %d
                        IP address of the last login failure: %s
                        Current device IP address: %s
                        Number of temporary lockouts for the user: %d
                        Last failure was before: %d ms
                        -----
                        """,
                loginFailures.getNumFailures(),
                loginFailures.getLastIPFailure(),
                ipAddressContext.getData().map(IPAddress::toString).orElse("unknown"),
                loginFailures.getNumTemporaryLockouts(),
                Time.currentTimeMillis() - loginFailures.getLastFailure()
        );

        logger.debugf("AI login failures request: %s", request);
        return request;
    }

    @Override
    public Optional<Double> evaluate() {
        var realm = session.getContext().getRealm();
        if (realm == null) {
            logger.debug("Context realm is null");
            return Optional.empty();
        }

        var user = Optional.ofNullable(session.getContext().getAuthenticationSession())
                .map(AuthenticationSessionModel::getAuthenticatedUser);
        if (user.isEmpty()) {
            logger.debug("Context user is null");
            return Optional.empty();
        }

        var loginFailures = session.loginFailures().getUserLoginFailure(realm, user.get().getId());
        if (loginFailures == null) {
            logger.debug("Cannot obtain login failures");
            return Optional.empty();

        }

        Optional<Double> evaluatedRisk = aiEngine.getRisk(request(loginFailures));
        evaluatedRisk.ifPresent(risk -> logger.debugf("AI request was successful. Evaluated risk: %f", risk));

        return evaluatedRisk;
    }
}
