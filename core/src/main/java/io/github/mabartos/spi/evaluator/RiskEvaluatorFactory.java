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
package io.github.mabartos.spi.evaluator;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

/**
 * Generic factory for the risk evaluators with the predefined attributes
 */
public interface RiskEvaluatorFactory extends ProviderFactory<RiskEvaluator>, EnvironmentDependentProviderFactory, ConfiguredProvider {
    String NAME_PREFIX = "Risk Evaluator - ";
    String TRUST_CONFIG = "adaptive-evaluator-trust";
    String ENABLED_CONFIG = "adaptive-evaluator-enabled";

    /**
     * Get display name of the risk evaluator
     */
    String getName();

    /**
     * Evaluation phase in which the risk score evaluation will be executed.
     * By default, reads the {@link EvaluationPhase} annotation from {@link #evaluatorClass()}.
     */
    default RiskEvaluator.EvaluationPhase evaluationPhase() {
        var annotation = evaluatorClass().getAnnotation(EvaluationPhase.class);
        if (annotation == null) {
            throw new IllegalStateException("Missing @EvaluationPhase annotation on " + evaluatorClass().getSimpleName());
        }
        return annotation.value();
    }

    /**
     * Evaluator class used for generating unique config keys (enabled/trust attributes)
     */
    Class<? extends RiskEvaluator> evaluatorClass();

    /**
     * Primary evaluation phase for admin console grouping.
     * Must match {@link RiskEvaluator#evaluationPhases()} on the evaluator implementation.
     */
    RiskEvaluator.EvaluationPhase evaluationPhase();

    /**
     * Short label for the realm admin Risk-based policies tab.
     */
    default String adminDisplayName() {
        return getName();
    }

    /**
     * Tooltip for the enabled toggle in the realm admin Risk-based policies tab.
     */
    default String adminEnabledHelpText() {
        return "Runs during the " + evaluationPhase().name().toLowerCase().replace('_', ' ')
                + " evaluation phase of adaptive authentication.";
    }

    /**
     * Tooltip for the trust weight field in the realm admin Risk-based policies tab.
     */
    default String adminTrustHelpText() {
        return "Adjust how strongly this evaluator influences the combined risk score.";
    }

    @Override
    default String getHelpText() {
        return getName().toLowerCase().contains("risk evaluator") ? getName() : NAME_PREFIX + getName();
    }

    /**
     * Default {@link ConfiguredProvider} schema for enabled/trust realm attributes.
     * Realm admin UI is built by {@code RiskBasedPoliciesUiTab}; this default keeps
     * {@link ConfiguredProvider} consumers aligned with {@link #adminDisplayName()} and admin help text.
     */
    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(isEnabledConfig(evaluatorClass()))
                .label(adminDisplayName())
                .helpText(adminEnabledHelpText())
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .property()
                .name(getTrustConfig(evaluatorClass()))
                .label(adminDisplayName() + " trust")
                .helpText(adminTrustHelpText())
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

    static String isEnabledConfig(Class<? extends RiskEvaluator> evaluator) {
        return ENABLED_CONFIG + "-" + evaluator.getSimpleName();
    }

    static String getTrustConfig(Class<? extends RiskEvaluator> evaluator) {
        return TRUST_CONFIG + "-" + evaluator.getSimpleName();
    }
}
