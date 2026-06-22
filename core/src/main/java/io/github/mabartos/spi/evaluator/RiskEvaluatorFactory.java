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
    String TRUST_CONFIG = "adaptive-evaluator-trust";
    String ENABLED_CONFIG = "adaptive-evaluator-enabled";

    /**
     * Short display name of the risk evaluator (realm admin labels, provider listing).
     */
    String getName();

    /**
     * Detailed description of what the evaluator does (realm admin enabled-toggle help text).
     */
    String getDescription();

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

    @Override
    default String getHelpText() {
        return getDescription();
    }

    /**
     * Default {@link ConfiguredProvider} schema for enabled/trust realm attributes.
     * Realm admin UI is built by {@code RiskBasedPoliciesUiTab}; this default keeps
     * {@link ConfiguredProvider} consumers aligned with {@link #getName()} and {@link #getDescription()}.
     */
    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(isEnabledConfig(evaluatorClass()))
                .label(getName())
                .helpText(getDescription())
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .property()
                .name(getTrustConfig(evaluatorClass()))
                .label(getName() + " trust")
                .helpText("Adjust how strongly this evaluator influences the combined risk score.")
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
