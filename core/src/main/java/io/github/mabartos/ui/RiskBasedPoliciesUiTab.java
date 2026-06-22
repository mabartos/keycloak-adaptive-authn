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
package io.github.mabartos.ui;

import io.github.mabartos.level.Trust;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import io.github.mabartos.evaluator.EvaluatorUtils;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.UiTabProvider;
import org.keycloak.services.ui.extend.UiTabProviderFactory;
import org.keycloak.utils.StringUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static io.github.mabartos.spi.engine.RiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static io.github.mabartos.spi.engine.RiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static io.github.mabartos.spi.engine.RiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static io.github.mabartos.spi.engine.RiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
import static io.github.mabartos.spi.evaluator.RiskEvaluatorFactory.getTrustConfig;
import static io.github.mabartos.spi.evaluator.RiskEvaluatorFactory.isEnabledConfig;

/**
 * Custom UI tab in administrator console to configure risk-based authentication properties
 */
public class RiskBasedPoliciesUiTab implements UiTabProvider, UiTabProviderFactory<ComponentModel> {
    private static final Logger logger = Logger.getLogger(RiskBasedPoliciesUiTab.class);

    private List<RiskEvaluatorFactory> riskEvaluatorFactories = Collections.emptyList();
    private List<RiskScoreAlgorithmFactory> algorithmFactories = Collections.emptyList();

    public static final String RISK_BASED_AUTHN_ENABLED_CONFIG = "adaptive-engine-enabled";
    public static final String AUDIT_EVENTS_ENABLED_CONFIG = "adaptive-audit-events-enabled";
    public static final String RISK_SCORE_ALGORITHM_CONFIG = "adaptive-engine-scoreAlgorithm";
    public static final String SIMPLE_FALLBACK_LEVEL_CONFIG = "adaptive-engine-simpleFallbackLevel";
    public static final String ADVANCED_FALLBACK_LEVEL_CONFIG = "adaptive-engine-advancedFallbackLevel";

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

        storeRealmAttribute(model, realm, RISK_BASED_AUTHN_ENABLED_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, AUDIT_EVENTS_ENABLED_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, EVALUATOR_TIMEOUT_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, EVALUATOR_RETRIES_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, RISK_SCORE_ALGORITHM_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, SIMPLE_FALLBACK_LEVEL_CONFIG, "onCreate");
        storeRealmAttribute(model, realm, ADVANCED_FALLBACK_LEVEL_CONFIG, "onCreate");

        // Set algorithm property defaults as realm attributes
        algorithmFactories.forEach(algFactory -> algFactory.getConfigProperties().forEach(prop -> {
            if (realm.getAttribute(prop.getName()) == null && prop.getDefaultValue() != null) {
                logger.debugf("onCreate setting algorithm default '%s' = '%s'", prop.getName(), prop.getDefaultValue());
                realm.setAttribute(prop.getName(), prop.getDefaultValue().toString());
            }
        }));

        riskEvaluatorFactories.forEach(evalFactory -> {
            var enabledKey = isEnabledConfig(evalFactory.evaluatorClass());
            doIfPresent(model.get(enabledKey), value -> {
                logger.debugf("onCreate storing evaluator enabled '%s' = '%s'", enabledKey, value);
                EvaluatorUtils.setEvaluatorEnabled(realm, evalFactory.evaluatorClass(), Boolean.parseBoolean(value));
            });

            var trustKey = getTrustConfig(evalFactory.evaluatorClass());
            doIfPresent(model.get(trustKey), value -> {
                logger.debugf("onCreate storing evaluator trust '%s' = '%s'", trustKey, value);
                EvaluatorUtils.storeEvaluatorTrust(realm, evalFactory.evaluatorClass(), Double.parseDouble(value));
            });
        });
    }

    private void storeRealmAttribute(ComponentModel model, RealmModel realm, String key, String context) {
        doIfPresent(model.get(key), value -> {
            logger.debugf("%s storing '%s' = '%s'", context, key, value);
            realm.setAttribute(key, value);
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

        storeRealmAttribute(newModel, realm, RISK_BASED_AUTHN_ENABLED_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, AUDIT_EVENTS_ENABLED_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, EVALUATOR_TIMEOUT_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, EVALUATOR_RETRIES_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, RISK_SCORE_ALGORITHM_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, SIMPLE_FALLBACK_LEVEL_CONFIG, "onUpdate");
        storeRealmAttribute(newModel, realm, ADVANCED_FALLBACK_LEVEL_CONFIG, "onUpdate");

        algorithmFactories.forEach(algFactory -> algFactory.getConfigProperties().forEach(prop -> {
            var oldVal = oldModel.get(prop.getName());
            var newVal = newModel.get(prop.getName());
            if (!Objects.equals(oldVal, newVal)) {
                if (StringUtil.isNotBlank(newVal)) {
                    logger.debugf("onUpdate storing algorithm prop '%s' = '%s' (was '%s')", prop.getName(), newVal, oldVal);
                    realm.setAttribute(prop.getName(), newVal);
                } else if (prop.getDefaultValue() != null) {
                    logger.debugf("onUpdate resetting algorithm prop '%s' to default '%s' (was '%s')", prop.getName(), prop.getDefaultValue(), oldVal);
                    realm.setAttribute(prop.getName(), prop.getDefaultValue().toString());
                }
            }
        }));

        riskEvaluatorFactories.forEach(f -> {
            var enabledKey = isEnabledConfig(f.evaluatorClass());
            var oldEnabled = oldModel.get(enabledKey);
            var newEnabled = newModel.get(enabledKey);
            if (!Objects.equals(oldEnabled, newEnabled)) {
                doIfPresent(newEnabled, value -> {
                    logger.debugf("onUpdate storing evaluator enabled '%s' = '%s' (was '%s')", enabledKey, value, oldEnabled);
                    EvaluatorUtils.setEvaluatorEnabled(realm, f.evaluatorClass(), Boolean.parseBoolean(value));
                });
            }

            var trustKey = getTrustConfig(f.evaluatorClass());
            var oldTrust = oldModel.get(trustKey);
            var newTrust = newModel.get(trustKey);
            if (!Objects.equals(oldTrust, newTrust)) {
                doIfPresent(newTrust, value -> {
                    logger.debugf("onUpdate storing evaluator trust '%s' = '%s' (was '%s')", trustKey, value, oldTrust);
                    EvaluatorUtils.storeEvaluatorTrust(realm, f.evaluatorClass(), Double.parseDouble(value));
                });
            }
        });
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        logger.tracef("validateConfiguration execution");

        // Populate missing config properties with defaults so the admin console
        // includes them in future form submissions
        populateMissingDefaults(model);

        validateInteger(model.get(EVALUATOR_TIMEOUT_CONFIG), "Timeout");
        validateInteger(model.get(EVALUATOR_RETRIES_CONFIG), "Retries");

        validateFallbackLevel(model.get(SIMPLE_FALLBACK_LEVEL_CONFIG), SimpleRiskLevels.getSimpleLevelNames(), "Simple fallback risk level");
        validateFallbackLevel(model.get(ADVANCED_FALLBACK_LEVEL_CONFIG), AdvancedRiskLevels.getAdvancedLevelNames(), "Advanced fallback risk level");

        riskEvaluatorFactories.forEach(f -> {
            try {
                var value = model.get(getTrustConfig(f.evaluatorClass()));
                if (StringUtil.isBlank(value)) return; // default value is an empty string

                var trust = Double.parseDouble(value);
                if (!Trust.isValid(trust)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new ComponentValidationException("Risk Trust levels must be double values in range [0.0, 1.0]");
            }
        });
    }

    /**
     * Ensures all config properties defined by {@link #getConfigProperties()} exist in the component model.
     * The admin console only submits keys already present in the component config,
     * so missing properties must be populated with defaults to be editable.
     */
    private void populateMissingDefaults(ComponentModel model) {
        getConfigProperties().forEach(prop -> {
            if (model.get(prop.getName()) == null && prop.getDefaultValue() != null) {
                model.getConfig().putSingle(prop.getName(), prop.getDefaultValue().toString());
            }
        });
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

    protected void validateFallbackLevel(String value, List<String> validLevels, String attributeDisplayName) {
        if (StringUtil.isNotBlank(value) && !validLevels.contains(value)) {
            throw new ComponentValidationException(
                    String.format("%s must be one of: %s", attributeDisplayName, String.join(", ", validLevels)));
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
                .name(AUDIT_EVENTS_ENABLED_CONFIG)
                .label("Risk evaluation audit events")
                .helpText("Persist risk evaluation summaries as user events (login and continuous remediation). "
                        + "Requires Realm settings → Events → User events: Save user events ON and "
                        + "Custom required action listed under Saved event types.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
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
                .property()
                .name(RISK_SCORE_ALGORITHM_CONFIG)
                .label("Risk score algorithm")
                .helpText("Algorithm used to compute the overall risk score from individual evaluator results")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(algorithmFactories.stream().map(RiskScoreAlgorithmFactory::getId).toList())
                .defaultValue(algorithmFactories.isEmpty() ? null : algorithmFactories.getFirst().getId())
                .add()
                .property()
                .name(SIMPLE_FALLBACK_LEVEL_CONFIG)
                .label("Fallback risk level (simple)")
                .helpText("Risk level used when the risk engine fails to evaluate. Applied to simple (3-level) conditions.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(SimpleRiskLevels.getSimpleLevelNames())
                .defaultValue(SimpleRiskLevels.MEDIUM)
                .add()
                .property()
                .name(ADVANCED_FALLBACK_LEVEL_CONFIG)
                .label("Fallback risk level (advanced)")
                .helpText("Risk level used when the risk engine fails to evaluate. Applied to advanced (5-level) conditions.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(AdvancedRiskLevels.getAdvancedLevelNames())
                .defaultValue(AdvancedRiskLevels.MEDIUM)
                .add()
                .build();

        // Add Algorithm-specific Properties
        list.addAll(algorithmFactories.stream()
                .flatMap(f -> f.getConfigProperties().stream())
                .toList());

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
        this.riskEvaluatorFactories = factory.getProviderFactoriesStream(RiskEvaluator.class).map(f -> (RiskEvaluatorFactory) f).toList();
        this.algorithmFactories = factory.getProviderFactoriesStream(RiskScoreAlgorithm.class)
                .map(f -> (RiskScoreAlgorithmFactory) f)
                .sorted(Comparator.comparingInt(RiskScoreAlgorithmFactory::order).reversed())
                .toList();
    }

    @Override
    public void close() {

    }


}
