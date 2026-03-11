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
package io.github.mabartos.level;

import io.github.mabartos.spi.level.SimpleRiskLevels;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Abstract factory for the risk level conditions to simplify their creation.
 * Risk levels are obtained directly from the RiskScoreAlgorithm at runtime.
 */
public abstract class AbstractRiskLevelConditionFactory implements ConditionalAuthenticatorFactory {
    public static final String LEVEL_CONFIG = "level-config";
    private static ConditionalAuthenticator RISK_LEVELS_CONDITION;

    /**
     * Determines whether to use simple (3-level) or advanced (5-level) risk levels.
     * @return true for advanced (5-level), false for simple (3-level)
     */
    public abstract boolean isAdvanced();

    @Override
    public ConditionalAuthenticator getSingleton() {
        if (RISK_LEVELS_CONDITION == null) {
            RISK_LEVELS_CONDITION = new RiskLevelCondition(isAdvanced());
        }
        return RISK_LEVELS_CONDITION;
    }

    @Override
    public String getDisplayType() {
        return String.format("Condition - Risk Level (%s)",
            isAdvanced() ? AdvancedRiskLevels.getDescription() : SimpleRiskLevels.getDescription());
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        var levelNames = isAdvanced()
            ? AdvancedRiskLevels.getAdvancedLevelNames()
            : SimpleRiskLevels.getSimpleLevelNames();

        return String.format("%s risk condition (%s). Thresholds automatically calibrated based on the algorithm.",
            isAdvanced() ? AdvancedRiskLevels.getDescription() : SimpleRiskLevels.getDescription(),
            String.join(", ", levelNames));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // Provide static level names - actual thresholds come from algorithm at runtime
        var levelNames = isAdvanced()
            ? AdvancedRiskLevels.getAdvancedLevelNames()
            : SimpleRiskLevels.getSimpleLevelNames();

        return ProviderConfigurationBuilder.create()
                .property()
                .name(LEVEL_CONFIG)
                .options(levelNames)
                .label(LEVEL_CONFIG)
                .helpText(LEVEL_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.LIST_TYPE)
                .add()
                .build();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No initialization needed - risk levels come from algorithm at runtime
    }

    @Override
    public void close() {

    }
}
