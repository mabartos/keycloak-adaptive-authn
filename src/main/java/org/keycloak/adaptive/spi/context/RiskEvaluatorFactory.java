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

    String getName();

    @Override
    default String getHelpText() {
        return getName().toLowerCase().contains("risk evaluator") ? getName() : NAME_PREFIX + getName();
    }

    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(getWeightConfig(getName()))
                .label(getName() + " Risk Weight")
                .helpText(WEIGHT_CONFIG + ".tooltip")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(Weight.DEFAULT)
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

    static String getWeightConfig(String evaluatorName) {
        return WEIGHT_CONFIG + "-" + evaluatorName;
    }
}
