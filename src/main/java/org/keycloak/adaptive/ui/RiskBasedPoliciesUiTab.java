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
package org.keycloak.adaptive.ui;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ui.extend.UiTabProvider;
import org.keycloak.services.ui.extend.UiTabProviderFactory;
import org.keycloak.utils.StringUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.keycloak.adaptive.engine.MutinyRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.MutinyRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.MutinyRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.MutinyRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
import static org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory.getWeightConfig;
import static org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory.isEnabledConfig;

/**
 * Custom UI tab in administrator console to configure risk-based authentication properties
 */
public class RiskBasedPoliciesUiTab implements UiTabProvider, UiTabProviderFactory<ComponentModel> {
    private static final Logger logger = Logger.getLogger(RiskBasedPoliciesUiTab.class);

    private List<RiskLevelsFactory> riskLevelsFactories = Collections.emptyList();
    private List<RiskEvaluatorFactory> riskEvaluatorFactories = Collections.emptyList();

    public static final String RISK_BASED_AUTHN_ENABLED_CONFIG = "riskBasedAuthnEnabled";
    public static final String RISK_LEVEL_PROVIDER_CONFIG = "riskLevelProvider";

    @Override
    public String getId() {
        return "Risk-based policies";
    }

    @Override
    public String getPath() {
        return "/:realm/authentication/:tab?";
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("tab", "risk-based-policies");
        return params;
    }

    @Override
    public String getHelpText() {
        return "Risk-based policies";
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        logger.debugf("onCreate execution");

        updateRiskBasedLevel(realm, model);

        doIfPresent(model.get(RISK_BASED_AUTHN_ENABLED_CONFIG), value -> realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, value));
        doIfPresent(model.get(EVALUATOR_TIMEOUT_CONFIG), value -> realm.setAttribute(EVALUATOR_TIMEOUT_CONFIG, value));
        doIfPresent(model.get(EVALUATOR_RETRIES_CONFIG), value -> realm.setAttribute(EVALUATOR_RETRIES_CONFIG, value));

        riskEvaluatorFactories.forEach(evalFactory -> {
            // Enabled
            var enabled = model.get(isEnabledConfig(evalFactory.evaluatorClass()));
            doIfPresent(enabled, value -> {
                logger.debugf("stored state '%s' for evaluator '%s' ('%s')", value, evalFactory.getName(), evalFactory.evaluatorClass().getSimpleName());
                EvaluatorUtils.setEvaluatorEnabled(session, evalFactory.evaluatorClass(), Boolean.parseBoolean(value));
            });

            var weight = model.get(getWeightConfig(evalFactory.evaluatorClass()));
            doIfPresent(weight, value -> {
                logger.debugf("putting weight '%f' for evaluator '%s' ('%s')", value, evalFactory.getName(),evalFactory.evaluatorClass());
                EvaluatorUtils.storeEvaluatorWeight(session, evalFactory.evaluatorClass(), Double.parseDouble(value));
            });
        });
    }

    private void doIfPresent(String value, Consumer<String> operation) {
        Optional.ofNullable(value)
                .filter(StringUtil::isNotBlank)
                .ifPresent(operation);
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        logger.debugf("onUpdate execution");

        updateRiskBasedLevel(realm, newModel);
        doIfPresent(newModel.get(RISK_BASED_AUTHN_ENABLED_CONFIG), value -> realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, value));
        doIfPresent(newModel.get(EVALUATOR_TIMEOUT_CONFIG), value -> realm.setAttribute(EVALUATOR_TIMEOUT_CONFIG, value));
        doIfPresent(newModel.get(EVALUATOR_RETRIES_CONFIG), value -> realm.setAttribute(EVALUATOR_RETRIES_CONFIG, value));

        riskEvaluatorFactories.forEach(f -> {
            {
                var oldEnabled = oldModel.get(isEnabledConfig(f.evaluatorClass()));
                var newEnabled = newModel.get(isEnabledConfig(f.evaluatorClass()));
                if (!Objects.equals(oldEnabled, newEnabled)) {
                    doIfPresent(newEnabled, value -> {
                        logger.debugf("setting new value for '%s' = '%s'", isEnabledConfig(f.evaluatorClass()), Boolean.parseBoolean(value));
                        EvaluatorUtils.setEvaluatorEnabled(session, f.evaluatorClass(), Boolean.parseBoolean(value));
                    });
                }
            }

            {
                var oldWeight = oldModel.get(getWeightConfig(f.evaluatorClass()));
                var newWeight = newModel.get(getWeightConfig(f.evaluatorClass()));
                if (!Objects.equals(oldWeight, newWeight)) {
                    doIfPresent(newWeight, value -> {
                        logger.debugf("setting new value for '%s' = '%s'", getWeightConfig(f.evaluatorClass()), value);
                        EvaluatorUtils.storeEvaluatorWeight(session, f.evaluatorClass(), Double.parseDouble(value));
                    });
                }
            }
        });
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        logger.debugf("validateConfiguration execution");

        validateInteger(model.get(EVALUATOR_TIMEOUT_CONFIG), "Timeout");
        validateInteger(model.get(EVALUATOR_RETRIES_CONFIG), "Retries");

        riskEvaluatorFactories.forEach(f -> {
            try {
                var value = model.get(getWeightConfig(f.evaluatorClass()));
                if (StringUtil.isBlank(value)) return; // default value is an empty string

                var weight = Double.parseDouble(value);
                if (!Risk.isValid(weight)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new ComponentValidationException("Risk Weights must be double values in range (0,1>");
            }
        });
    }

    protected void updateRiskBasedLevel(RealmModel realm, ComponentModel model) {
        var providerId = riskLevelsFactories.stream()
                .filter(f -> f.getHelpText().equals(model.get(RISK_LEVEL_PROVIDER_CONFIG)))
                .findAny()
                .map(ProviderFactory::getId)
                .orElse("");

        realm.setAttribute(RISK_LEVEL_PROVIDER_CONFIG, providerId);
    }

    protected void validateInteger(String value, String attributeDisplayName) {
        try {
            var timeout = Integer.parseInt(value);
            if (timeout < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ComponentValidationException(String.format("%s must be positive number or 0", attributeDisplayName));
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final List<ProviderConfigProperty> list = ProviderConfigurationBuilder.create().property()
                .name(RISK_BASED_AUTHN_ENABLED_CONFIG)
                .label("enabled")
                .helpText("Whether risk-based authentication should be enabled")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .property()
                .name(RISK_LEVEL_PROVIDER_CONFIG)
                .label("Risk levels")
                .helpText("Which risk levels will be used for Risk Level conditions")
                .type(ProviderConfigProperty.LIST_TYPE)
                .defaultValue(riskLevelsFactories.stream().findFirst().map(ConfiguredProvider::getHelpText).orElse("No Levels"))
                .options(riskLevelsFactories.stream().map(ConfiguredProvider::getHelpText).toList())
                .add()
                .property()
                .name(EVALUATOR_TIMEOUT_CONFIG)
                .label("Risk evaluator timeout (ms)")
                .helpText("Timeout for evaluating risk score")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(DEFAULT_EVALUATOR_TIMEOUT.toMillis())
                .add()
                .property()
                .name(EVALUATOR_RETRIES_CONFIG)
                .label("Risk evaluator retries")
                .helpText("Number of retries for evaluating risk score")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(DEFAULT_EVALUATOR_RETRIES)
                .add()
                .build();

        // Add Risk Evaluator Properties
        list.addAll(riskEvaluatorFactories.stream()
                .flatMap(f -> f.getConfigProperties().stream())
                .toList());

        return list;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.riskLevelsFactories = factory.getProviderFactoriesStream(RiskLevelsProvider.class).map(f -> (RiskLevelsFactory) f).toList();
        this.riskEvaluatorFactories = factory.getProviderFactoriesStream(RiskEvaluator.class).map(f -> (RiskEvaluatorFactory) f).toList();
    }

    @Override
    public void close() {

    }


}
