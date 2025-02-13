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
package org.keycloak.adaptive.evaluator.device;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.device.DefaultDeviceContextFactory;
import org.keycloak.adaptive.context.device.DeviceContext;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.spi.ai.AiNlpEngine;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;

/**
 * Risk evaluator for checking device properties evaluated by AI NLP engine
 */
public class AiDeviceRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiDeviceRiskEvaluator.class);

    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final AiNlpEngine aiEngine;

    public AiDeviceRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiNlpEngine.class);
    }

    @Override
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, AiDeviceRiskEvaluatorFactory.class, 0.15);
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
    public Optional<Double> evaluate() {
        if (aiEngine == null) {
            logger.warnf("Cannot find AI engine");
            return Optional.empty();
        }

        var deviceRepresentation = deviceContext.getData();
        if (deviceRepresentation.isPresent()) {
            Optional<Double> evaluatedRisk = aiEngine.getRisk(request(deviceRepresentation.get()));
            evaluatedRisk.ifPresent(risk -> logger.debugf("AI request was successful. Evaluated risk: %f", risk));
            return evaluatedRisk;
        } else {
            logger.warnf("Device representation is null");
        }
        return Optional.empty();
    }
}
