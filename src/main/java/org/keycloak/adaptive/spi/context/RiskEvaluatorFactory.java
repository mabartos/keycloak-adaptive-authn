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
package org.keycloak.adaptive.spi.context;

import org.keycloak.Config;
import org.keycloak.adaptive.level.Weight;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

public interface RiskEvaluatorFactory extends ProviderFactory<RiskEvaluator>, EnvironmentDependentProviderFactory, ConfiguredProvider {
    String NAME_PREFIX = "Risk Evaluator - ";
    String WEIGHT_CONFIG = "riskEvaluatorWeightConfig";
    String ENABLED_CONFIG = "riskEvaluatorEnabledConfig";

    String getName();

    @Override
    default String getHelpText() {
        return getName().toLowerCase().contains("risk evaluator") ? getName() : NAME_PREFIX + getName();
    }

    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(isEnabledConfig(getClass()))
                .label(getName() + " Enabled")
                .helpText(ENABLED_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .property()
                .name(getWeightConfig(getClass()))
                .label(getName() + " Risk Weight")
                .helpText(WEIGHT_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();
    }

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    default void close() {

    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return true;
    }

    static String isEnabledConfig(Class<? extends RiskEvaluatorFactory> factory) {
        return ENABLED_CONFIG + "-" + factory.getSimpleName();
    }

    static String getWeightConfig(Class<? extends RiskEvaluatorFactory> factory) {
        return WEIGHT_CONFIG + "-" + factory.getSimpleName();
    }
}
