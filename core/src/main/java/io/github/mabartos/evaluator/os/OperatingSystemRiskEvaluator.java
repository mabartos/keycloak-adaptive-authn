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
package io.github.mabartos.evaluator.os;

import io.github.mabartos.context.UserContexts;
import io.github.mabartos.context.os.OperatingSystemCondition;
import io.github.mabartos.context.os.OperatingSystemConditionFactory;
import io.github.mabartos.spi.evaluator.DeviceRiskEvaluator;
import io.github.mabartos.spi.level.Risk;
import jakarta.annotation.Nonnull;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;

/**
 * Risk evaluator for OS properties
 * Known OS = trust signal, unknown OS = moderate risk
 */
public class OperatingSystemRiskEvaluator extends DeviceRiskEvaluator {
    private final OperatingSystemCondition condition;

    public OperatingSystemRiskEvaluator(KeycloakSession session) {
        this.condition = UserContexts.getContextCondition(session, OperatingSystemConditionFactory.PROVIDER_ID);
    }

    @Override
    public Risk evaluate(@Nonnull RealmModel realm) {
        return condition.isDefaultKnownOs(realm)
            ? Risk.of(NEGATIVE_LOW, "Known OS - trust signal")
            : Risk.of(MEDIUM, "Unknown OS");
    }
}
