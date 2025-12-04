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
package org.keycloak.adaptive.evaluator.os;

import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.os.OperatingSystemCondition;
import org.keycloak.adaptive.context.os.OperatingSystemConditionFactory;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

/**
 * Risk evaluator for OS properties
 */
public class OperatingSystemRiskEvaluator extends AbstractRiskEvaluator {
    private final KeycloakSession session;
    private final OperatingSystemCondition condition;

    public OperatingSystemRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.condition = UserContexts.getContextCondition(session, OperatingSystemConditionFactory.PROVIDER_ID);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public double getDefaultWeight() {
        return Weight.LOW;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.BEFORE_AUTHN);
    }

    @Override
    public Risk evaluate() {
        return condition.isDefaultKnownOs() ? Risk.none() : Risk.of(Risk.INTERMEDIATE);
    }
}
