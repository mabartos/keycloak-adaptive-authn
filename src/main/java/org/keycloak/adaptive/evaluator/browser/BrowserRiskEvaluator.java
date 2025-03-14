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
package org.keycloak.adaptive.evaluator.browser;

import org.keycloak.adaptive.context.UserContexts;
import org.keycloak.adaptive.context.browser.BrowserCondition;
import org.keycloak.adaptive.context.browser.BrowserConditionFactory;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.adaptive.spi.evaluator.AbstractRiskEvaluator;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

/**
 * Risk evaluator for browser properties
 */
public class BrowserRiskEvaluator extends AbstractRiskEvaluator {
    private final KeycloakSession session;
    private final BrowserCondition browserCondition;

    public BrowserRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.browserCondition = UserContexts.getContextCondition(session, BrowserConditionFactory.PROVIDER_ID);
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.BEFORE_AUTHN);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.LOW;
    }

    @Override
    public Risk evaluate() {
        return browserCondition.isDefaultKnownBrowser() ? Risk.none() : Risk.of(Risk.INTERMEDIATE);
    }
}
