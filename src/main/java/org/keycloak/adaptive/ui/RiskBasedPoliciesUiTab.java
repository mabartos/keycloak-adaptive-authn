package org.keycloak.adaptive.ui;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.component.ComponentModel;
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

public class RiskBasedPoliciesUiTab implements UiTabProvider, UiTabProviderFactory<ComponentModel> {
    private List<RiskLevelsFactory> riskLevelsFactories = Collections.emptyList();
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
        var providerId = riskLevelsFactories.stream()
                .filter(f -> f.getHelpText().equals(model.get(RISK_LEVEL_PROVIDER_CONFIG)))
                .findAny()
                .map(ProviderFactory::getId)
                .orElse("");

        realm.setAttribute(RISK_BASED_AUTHN_ENABLED_CONFIG, model.get(RISK_BASED_AUTHN_ENABLED_CONFIG));
        realm.setAttribute(RISK_LEVEL_PROVIDER_CONFIG, providerId);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        final ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        builder.property()
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
                .add();
        return builder.build();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.riskLevelsFactories = factory.getProviderFactoriesStream(RiskLevelsProvider.class).map(f -> (RiskLevelsFactory) f).toList();
    }

    @Override
    public void close() {

    }


}
