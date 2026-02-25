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
package io.github.mabartos.evaluator.browser;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.browser.BrowserCondition;
import io.github.mabartos.context.browser.BrowserConditionFactory;
import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.DeviceRiskEvaluator;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Risk evaluator for browser properties
 */
public class BrowserRiskEvaluator extends DeviceRiskEvaluator {
    private final BrowserCondition browserCondition;

    public BrowserRiskEvaluator(KeycloakSession session) {
        this.browserCondition = UserContexts.getContextCondition(session, BrowserConditionFactory.PROVIDER_ID);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm) {
        return browserCondition.isDefaultKnownBrowser(realm) ? Risk.none() : Risk.of(Risk.INTERMEDIATE);
    }

    @Override
    public double getDefaultWeight() {
        return Weight.LOW;
    }
}
