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

import static org.keycloak.adaptive.spi.context.RiskEvaluatorFactory.getWeightConfig;

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

        var providerId = riskLevelsFactories.stream()
                .filter(f -> f.getHelpText().equals(model.get(RISK_LEVEL_PROVIDER_CONFIG)))
                .findAny()
                .map(ProviderFactory::getId)
                .orElse("");

        realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, model.get(RISK_BASED_AUTHN_ENABLED_CONFIG));
        realm.setAttribute(RISK_LEVEL_PROVIDER_CONFIG, providerId);

        riskEvaluatorFactories.forEach(f -> {
            var provider = f.create(session);
            model.put(getWeightConfig(f.getName()), Double.toString(provider.getWeight()));
            EvaluatorUtils.storeEvaluatorWeight(session, f.getName(), Double.parseDouble(model.get(getWeightConfig(f.getName()))));
            logger.debugf("putting weight '%f' for evaluator '%s'", provider.getWeight(), f.getName());
        });
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        logger.debugf("onUpdate execution");
        riskEvaluatorFactories.forEach(f -> {
            var oldConfig = oldModel.get(getWeightConfig(f.getName()));
            var newConfig = newModel.get(getWeightConfig(f.getName()));
            if (!Objects.equals(oldConfig, newConfig) && newConfig != null) {
                logger.debugf("setting new value for '%s' = '%s'", getWeightConfig(f.getName()), newConfig);
                EvaluatorUtils.storeEvaluatorWeight(session, f.getName(), Double.parseDouble(newConfig));
            }
        });
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        logger.debugf("validateConfiguration execution");
        riskEvaluatorFactories.forEach(f -> {
            try {
                var value = Double.parseDouble(model.get(getWeightConfig(f.getName())));
                if (!RiskEngine.isValidValue(value)) {
                    throw new ComponentValidationException("Risk Weights must be double values in range (0,1>");
                }
            } catch (NumberFormatException e) {
                throw new ComponentValidationException("Risk Weights must be double values in range (0,1>");
            }
        });
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
