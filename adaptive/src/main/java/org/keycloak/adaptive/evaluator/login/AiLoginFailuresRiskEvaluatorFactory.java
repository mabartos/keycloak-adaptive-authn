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

import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class AiLoginFailuresRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "ai-login-failures-risk-evaluator";
    public static final String NAME = "AI Login failures";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new AiLoginFailuresRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
