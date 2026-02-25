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
package io.github.mabartos.evaluator.device;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.device.DeviceRepresentationContext;
import io.github.mabartos.context.device.DeviceRepresentationContextFactory;
import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.ai.AiEngine;
import io.github.mabartos.spi.evaluator.DeviceRiskEvaluator;
import jakarta.annotation.Nonnull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.account.DeviceRepresentation;

/**
 * Risk evaluator for checking device properties evaluated by AI NLP engine
 */
public class AiDeviceRiskEvaluator extends DeviceRiskEvaluator {
    private static final Logger logger = Logger.getLogger(AiDeviceRiskEvaluator.class);

    private final DeviceRepresentationContext deviceContext;
    private final AiEngine aiEngine;

    public AiDeviceRiskEvaluator(KeycloakSession session) {
        this.deviceContext = UserContexts.getContext(session, DeviceRepresentationContextFactory.PROVIDER_ID);
        this.aiEngine = session.getProvider(AiEngine.class);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.LOW;
    }

    @Override
    public boolean allowRetries() {
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

        logger.tracef("AI device request: %s", request);
        return request;
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm) {
        if (aiEngine == null) {
            return Risk.invalid("Cannot find AI engine");
        }

        var deviceRepresentation = deviceContext.getData(realm);
        if (deviceRepresentation.isPresent()) {
            return aiEngine.getRisk(request(deviceRepresentation.get()));
        }
        return Risk.invalid("Cannot find device information");
    }
}
