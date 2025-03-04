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
import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.device.DefaultDeviceContextFactory;
import org.keycloak.adaptive.context.device.DeviceContext;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.ai.AiEngine;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;
import java.util.Set;

/**
 * Risk evaluator for checking device properties evaluated by AI NLP engine
 */
public class AiDeviceRiskEvaluator extends AbstractRiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiDeviceRiskEvaluator.class);

    private final KeycloakSession session;
    private final DeviceContext deviceContext;
    private final AiEngine aiEngine;

    public AiDeviceRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.deviceContext = UserContexts.getContext(session, DefaultDeviceContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiEngine.class);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public double getDefaultWeight() {
        return 0.15;
    }

    @Override
    public boolean allowRetries() {
        return false;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.BEFORE_AUTHN);
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

        logger.tracef("AI device request: %s", request);
        return request;
    }

    @Override
    public Risk evaluate() {
        if (aiEngine == null) {
            logger.warnf("Cannot find AI engine");
            return Risk.invalid();
        }

        var deviceRepresentation = deviceContext.getData();
        if (deviceRepresentation.isPresent()) {
            return aiEngine.getRisk(request(deviceRepresentation.get()));
        } else {
            logger.warnf("Device representation is null");
        }
        return Risk.invalid();
    }
}
