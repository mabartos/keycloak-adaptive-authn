package org.keycloak.adaptive.ui;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.adaptive.spi.engine.RiskEngine;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_RETRIES;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.DEFAULT_EVALUATOR_TIMEOUT;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_RETRIES_CONFIG;
import static org.keycloak.adaptive.engine.DefaultRiskEngineFactory.EVALUATOR_TIMEOUT_CONFIG;
import static org.keycloak.adaptive.spi.context.RiskEvaluatorFactory.getWeightConfig;
import static org.keycloak.adaptive.spi.context.RiskEvaluatorFactory.isEnabledConfig;

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
        realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, model.get(RISK_BASED_AUTHN_ENABLED_CONFIG));
        realm.setAttribute(EVALUATOR_TIMEOUT_CONFIG, model.get(EVALUATOR_TIMEOUT_CONFIG));
        realm.setAttribute(EVALUATOR_RETRIES_CONFIG, model.get(EVALUATOR_RETRIES_CONFIG));

        riskEvaluatorFactories.forEach(f -> {
            var provider = f.create(session);

            model.put(isEnabledConfig(f.getClass()), provider.isEnabled());
            EvaluatorUtils.setEvaluatorEnabled(session, f.getClass(), Boolean.parseBoolean(model.get(isEnabledConfig(f.getClass()))));
            logger.debugf("stored state '%s' for evaluator '%s'", provider.isEnabled(), f.getName());

            model.put(getWeightConfig(f.getClass()), Double.toString(provider.getWeight()));
            EvaluatorUtils.storeEvaluatorWeight(session, f.getClass(), Double.parseDouble(model.get(getWeightConfig(f.getClass()))));
            logger.debugf("putting weight '%f' for evaluator '%s'", provider.getWeight(), f.getName());
        });
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        logger.debugf("onUpdate execution");

        updateRiskBasedLevel(realm, newModel);
        realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, newModel.get(RISK_BASED_AUTHN_ENABLED_CONFIG));
        realm.setAttribute(EVALUATOR_TIMEOUT_CONFIG, newModel.get(EVALUATOR_TIMEOUT_CONFIG));
        realm.setAttribute(EVALUATOR_RETRIES_CONFIG, newModel.get(EVALUATOR_RETRIES_CONFIG));

        riskEvaluatorFactories.forEach(f -> {
            var oldEnabled = oldModel.get(isEnabledConfig(f.getClass()));
            var newEnabled = oldModel.get(isEnabledConfig(f.getClass()));
            if (!Objects.equals(oldEnabled, newEnabled) && newEnabled != null) {
                var enabled = Boolean.parseBoolean(newEnabled);
                logger.debugf("setting new value for '%s' = '%s'", isEnabledConfig(f.getClass()), enabled);
                EvaluatorUtils.setEvaluatorEnabled(session, f.getClass(), enabled);
            }

            var oldWeight = oldModel.get(getWeightConfig(f.getClass()));
            var newWeight = newModel.get(getWeightConfig(f.getClass()));
            if (!Objects.equals(oldWeight, newWeight) && newWeight != null) {
                logger.debugf("setting new value for '%s' = '%s'", getWeightConfig(f.getClass()), newWeight);
                EvaluatorUtils.storeEvaluatorWeight(session, f.getClass(), Double.parseDouble(newWeight));
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
                var value = Double.parseDouble(model.get(getWeightConfig(f.getClass())));
                if (!RiskEngine.isValidValue(value)) {
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
                .defaultValue(DEFAULT_EVALUATOR_TIMEOUT)
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
