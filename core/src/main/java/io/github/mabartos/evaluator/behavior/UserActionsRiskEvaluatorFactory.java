package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.List;
import java.util.Optional;

public class UserActionsRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "user-actions-continuous";
    protected static final String NAME = "User actions continuous evaluator";

    public static final String LOOKUP_TIME_MINUTES_PROP = "user.actions.lookup.time.minutes";
    private static final String LOOKUP_TIME_MINUTES_CONFIG = "lookup.time.minutes";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return UserActionsRiskEvaluator.class;
    }

    @Override
    public UserActionsRiskEvaluator create(KeycloakSession session) {
        return new UserActionsRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = RiskEvaluatorFactory.super.getConfigProperties();

        properties.add(ProviderConfigurationBuilder.create()
                .property()
                .name(LOOKUP_TIME_MINUTES_CONFIG)
                .label("Lookup Time Window (minutes)")
                .helpText("Time window in minutes to look back for sensitive user actions. Default is 30 minutes.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("30")
                .add()
                .build()
                .get(0));

        return properties;
    }

    public static Optional<Integer> getLookupTimeMinutes() {
        return Configuration.getOptionalValue(LOOKUP_TIME_MINUTES_PROP)
                .map(Integer::parseInt);
    }
}
