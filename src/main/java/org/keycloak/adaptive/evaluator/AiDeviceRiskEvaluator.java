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

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DefaultDeviceContextFactory;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;

public class AiDeviceRiskEvaluator implements RiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiDeviceRiskEvaluator.class);

    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final AiNlpEngine aiEngine;

    private Double risk;

    public AiDeviceRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiNlpEngine.class);
    }

    @Override
    public Optional<Double> getRiskValue() {
        return Optional.ofNullable(risk);
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, AiDeviceRiskEvaluatorFactory.class, Weight.IMPORTANT);
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, AiDeviceRiskEvaluatorFactory.class);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected static String request(DeviceRepresentation device) {
        // we should be careful about the message poisoning
        var request = String.format("""
                        Give me the overall risk that the device trying to authenticate is fraud based on its parameters.
                        -----
                        IP Address: %s
                        Browser: %s
                        Operating System: %s
                        OS version: %s
                        Is Mobile? : %s
                        Last access: %s
                        -----
                        """,
                device.getIpAddress(),
                device.getBrowser(),
                device.getOs(),
                device.getOsVersion(),
                device.isMobile(),
                device.getLastAccess()
        );

        logger.debugf("AI device request: %s", request);
        return request;
    }

    @Override
    public void evaluate() {
        if (aiEngine == null) {
            logger.warnf("Cannot find AI engine");
            return;
        }

        var deviceRepresentation = deviceContext.getData();
        if (deviceRepresentation == null) {
            logger.warnf("Device representation is null");
            return;
        }

        Optional<Double> evaluatedRisk = aiEngine.getRisk(request(deviceRepresentation));
        evaluatedRisk.ifPresent(risk -> {
            logger.debugf("AI request was successful. Evaluated risk: %f", risk);
            this.risk = risk;
        });

    }
}
