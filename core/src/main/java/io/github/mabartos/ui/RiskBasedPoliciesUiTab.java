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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final String TRUST_FIELD_HELP =
            "Weight from 0 to 1 (e.g. 0.75). 1 = full influence on the combined risk score.";

    private List<RiskEvaluatorFactory> riskEvaluatorFactories = Collections.emptyList();
    private List<RiskScoreAlgorithmFactory> algorithmFactories = Collections.emptyList();

    public static final String RISK_BASED_AUTHN_ENABLED_CONFIG = "adaptive-engine-enabled";
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
        return "Configure the adaptive risk engine, scoring algorithm, and risk evaluators for this realm.";
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        logger.debugf("onCreate execution");

        storeRealmAttributeFromModel(model, realm, RISK_BASED_AUTHN_ENABLED_CONFIG, "onCreate");
        storeRealmAttributeFromModel(model, realm, EVALUATOR_TIMEOUT_CONFIG, "onCreate");
        storeRealmAttributeFromModel(model, realm, EVALUATOR_RETRIES_CONFIG, "onCreate");
        storeRealmAttributeFromModel(model, realm, RISK_SCORE_ALGORITHM_CONFIG, "onCreate");
        storeRealmAttributeFromModel(model, realm, SIMPLE_FALLBACK_LEVEL_CONFIG, "onCreate");
        storeRealmAttributeFromModel(model, realm, ADVANCED_FALLBACK_LEVEL_CONFIG, "onCreate");

        algorithmFactories.forEach(algFactory -> algFactory.getConfigProperties().forEach(prop -> {
                    if (realm.getAttribute(prop.getName()) == null && prop.getDefaultValue() != null) {
                        logger.debugf("onCreate setting algorithm default '%s' = '%s'", prop.getName(), prop.getDefaultValue());
                        realm.setAttribute(prop.getName(), prop.getDefaultValue().toString());
                    }
                }));

        riskEvaluatorFactories.forEach(evalFactory -> persistEvaluatorSettings(model, realm, evalFactory, "onCreate"));
    }

    /**
     * Persists a realm attribute when the admin form submitted the key (including {@code false} for booleans).
     */
    private void storeRealmAttributeFromModel(ComponentModel model, RealmModel realm, String key, String context) {
        if (!model.contains(key)) {
            return;
        }
        var value = model.get(key);
        if (value == null) {
            value = "";
        }
        logger.debugf("%s storing '%s' = '%s'", context, key, value);
        realm.setAttribute(key, value);
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        logger.debugf("onUpdate execution");

        storeRealmAttributeFromModel(newModel, realm, RISK_BASED_AUTHN_ENABLED_CONFIG, "onUpdate");
        storeRealmAttributeFromModel(newModel, realm, EVALUATOR_TIMEOUT_CONFIG, "onUpdate");
        storeRealmAttributeFromModel(newModel, realm, EVALUATOR_RETRIES_CONFIG, "onUpdate");
        storeRealmAttributeFromModel(newModel, realm, RISK_SCORE_ALGORITHM_CONFIG, "onUpdate");
        storeRealmAttributeFromModel(newModel, realm, SIMPLE_FALLBACK_LEVEL_CONFIG, "onUpdate");
        storeRealmAttributeFromModel(newModel, realm, ADVANCED_FALLBACK_LEVEL_CONFIG, "onUpdate");

        algorithmFactories.forEach(algFactory -> algFactory.getConfigProperties().forEach(prop -> {
                    var oldVal = oldModel.get(prop.getName());
                    var newVal = newModel.get(prop.getName());
                    if (!Objects.equals(oldVal, newVal)) {
                        if (StringUtil.isNotBlank(newVal)) {
                            logger.debugf("onUpdate storing algorithm prop '%s' = '%s' (was '%s')", prop.getName(), newVal, oldVal);
                            realm.setAttribute(prop.getName(), newVal);
                        } else if (prop.getDefaultValue() != null) {
                            logger.debugf("onUpdate resetting algorithm prop '%s' to default '%s' (was '%s')",
                                    prop.getName(), prop.getDefaultValue(), oldVal);
                            realm.setAttribute(prop.getName(), prop.getDefaultValue().toString());
                        }
                    }
                }));

        riskEvaluatorFactories.forEach(f -> {
            var enabledKey = isEnabledConfig(f.evaluatorClass());
            if (!Objects.equals(oldModel.get(enabledKey), newModel.get(enabledKey)) && newModel.contains(enabledKey)) {
                var value = newModel.get(enabledKey);
                var enabled = value == null || Boolean.parseBoolean(value);
                logger.debugf("onUpdate storing evaluator enabled '%s' = '%s'", enabledKey, enabled);
                EvaluatorUtils.setEvaluatorEnabled(realm, f.evaluatorClass(), enabled);
            }

            var trustKey = getTrustConfig(f.evaluatorClass());
            var oldTrust = oldModel.get(trustKey);
            var newTrust = newModel.get(trustKey);
            if (!Objects.equals(oldTrust, newTrust) && newModel.contains(trustKey) && StringUtil.isNotBlank(newTrust)) {
                logger.debugf("onUpdate storing evaluator trust '%s' = '%s' (was '%s')", trustKey, newTrust, oldTrust);
                EvaluatorUtils.storeEvaluatorTrust(realm, f.evaluatorClass(), Double.parseDouble(newTrust));
            }
        });
    }

    private void persistEvaluatorSettings(ComponentModel model, RealmModel realm, RiskEvaluatorFactory evalFactory, String context) {
        var enabledKey = isEnabledConfig(evalFactory.evaluatorClass());
        if (model.contains(enabledKey)) {
            var value = model.get(enabledKey);
            var enabled = value == null || Boolean.parseBoolean(value);
            logger.debugf("%s storing evaluator enabled '%s' = '%s'", context, enabledKey, enabled);
            EvaluatorUtils.setEvaluatorEnabled(realm, evalFactory.evaluatorClass(), enabled);
        }

        var trustKey = getTrustConfig(evalFactory.evaluatorClass());
        var trustValue = model.get(trustKey);
        if (model.contains(trustKey) && StringUtil.isNotBlank(trustValue)) {
            logger.debugf("%s storing evaluator trust '%s' = '%s'", context, trustKey, trustValue);
            EvaluatorUtils.storeEvaluatorTrust(realm, evalFactory.evaluatorClass(), Double.parseDouble(trustValue));
        }
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        logger.tracef("validateConfiguration execution");

        populateMissingDefaults(model);

        validateInteger(model.get(EVALUATOR_TIMEOUT_CONFIG), "Timeout");
        validateInteger(model.get(EVALUATOR_RETRIES_CONFIG), "Retries");

        validateFallbackLevel(model.get(SIMPLE_FALLBACK_LEVEL_CONFIG), SimpleRiskLevels.getSimpleLevelNames(), "Simple fallback risk level");
        validateFallbackLevel(model.get(ADVANCED_FALLBACK_LEVEL_CONFIG), AdvancedRiskLevels.getAdvancedLevelNames(), "Advanced fallback risk level");

        riskEvaluatorFactories.forEach(f -> {
            var value = model.get(getTrustConfig(f.evaluatorClass()));
            if (StringUtil.isBlank(value)) {
                return;
            }
            try {
                var trust = Double.parseDouble(value);
                if (!Trust.isValid(trust)) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new ComponentValidationException(
                        "Risk Trust levels must be double values in range [0.0, 1.0]");
            }
        });
    }

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
        return buildConfigProperties(algorithmFactories, riskEvaluatorFactories);
    }

    static List<ProviderConfigProperty> buildConfigProperties(
            List<RiskScoreAlgorithmFactory> algorithmFactories,
            List<RiskEvaluatorFactory> riskEvaluatorFactories) {
        var list = new ArrayList<ProviderConfigProperty>();

        list.addAll(ProviderConfigurationBuilder.create().property()
                .name(RISK_BASED_AUTHN_ENABLED_CONFIG)
                .label("Enable adaptive authentication")
                .helpText("Master switch for the adaptive risk engine on this realm. When off, risk evaluators do not run during login.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .property()
                .name(EVALUATOR_TIMEOUT_CONFIG)
                .label("Evaluator timeout ms")
                .helpText("Maximum time allowed for a single risk evaluator call. Evaluators exceeding this limit may be retried or skipped depending on retries.")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue((int) DEFAULT_EVALUATOR_TIMEOUT.toMillis())
                .add()
                .property()
                .name(EVALUATOR_RETRIES_CONFIG)
                .label("Evaluator retries")
                .helpText("How many times to retry an evaluator after timeout or transient failure before treating it as failed.")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(DEFAULT_EVALUATOR_RETRIES)
                .add()
                .property()
                .name(RISK_SCORE_ALGORITHM_CONFIG)
                .label("Score algorithm")
                .helpText("How per-phase evaluator evidence is combined into an overall score. Log-odds (Bayesian) is recommended; weighted average is deprecated.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(algorithmFactories.stream().map(RiskScoreAlgorithmFactory::getId).toList())
                .defaultValue(algorithmFactories.isEmpty() ? null : algorithmFactories.getFirst().getId())
                .add()
                .property()
                .name(SIMPLE_FALLBACK_LEVEL_CONFIG)
                .label("Fallback on error simple 3 levels")
                .helpText("Risk level applied when the engine cannot compute a score. Used by authentication conditions configured with the 3-level (LOW/MEDIUM/HIGH) model.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(SimpleRiskLevels.getSimpleLevelNames())
                .defaultValue(SimpleRiskLevels.MEDIUM)
                .add()
                .property()
                .name(ADVANCED_FALLBACK_LEVEL_CONFIG)
                .label("Fallback on error advanced 5 levels")
                .helpText("Risk level applied when the engine cannot compute a score. Used by conditions with the 5-level advanced model.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(AdvancedRiskLevels.getAdvancedLevelNames())
                .defaultValue(AdvancedRiskLevels.MEDIUM)
                .add()
                .build());

        algorithmFactories.stream()
                .flatMap(f -> f.getConfigProperties().stream())
                .forEach(list::add);

        for (var phase : RiskEvaluator.EvaluationPhase.values()) {
            appendEvaluatorPhase(list, riskEvaluatorFactories, phase);
        }

        return list;
    }

    private static void appendEvaluatorPhase(
            List<ProviderConfigProperty> list,
            List<RiskEvaluatorFactory> factories,
            RiskEvaluator.EvaluationPhase phase) {
        factories.stream()
                .filter(f -> f.evaluationPhase() == phase)
                .sorted(Comparator.comparing(RiskEvaluatorFactory::getName))
                .flatMap(f -> evaluatorAdminProperties(f).stream())
                .forEach(list::add);
    }

    private static List<ProviderConfigProperty> evaluatorAdminProperties(RiskEvaluatorFactory factory) {
        var evaluatorClass = factory.evaluatorClass();
        var properties = new ArrayList<ProviderConfigProperty>();
        properties.addAll(ProviderConfigurationBuilder.create()
                .property()
                .name(isEnabledConfig(evaluatorClass))
                .label(RiskEvaluatorUi.enabledLabel(factory))
                .helpText(RiskEvaluatorUi.enabledTooltip(factory))
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(true)
                .add()
                .build());
        properties.add(ProviderConfigurationBuilder.create()
                .property()
                .name(getTrustConfig(evaluatorClass))
                .label(RiskEvaluatorUi.trustLabel(factory))
                .helpText(RiskEvaluatorUi.trustTooltip() + " " + TRUST_FIELD_HELP)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build()
                .getFirst());
        return properties;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.riskEvaluatorFactories = factory.getProviderFactoriesStream(RiskEvaluator.class)
                .map(f -> (RiskEvaluatorFactory) f)
                .toList();
        this.algorithmFactories = factory.getProviderFactoriesStream(RiskScoreAlgorithm.class)
                .map(f -> (RiskScoreAlgorithmFactory) f)
                .sorted(Comparator.comparingInt(RiskScoreAlgorithmFactory::order).reversed())
                .toList();
    }

    @Override
    public void close() {

    }
}
