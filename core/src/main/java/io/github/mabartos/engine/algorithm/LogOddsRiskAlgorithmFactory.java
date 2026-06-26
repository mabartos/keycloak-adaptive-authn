package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class LogOddsRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "log-odds";

    /**
     * Default bias for the algorithm.
     * Negative bias makes the algorithm less aggressive, requiring more evidence to reach high risk levels.
     * Since overall risk combines evidence additively from both BEFORE_AUTHN and USER_KNOWN phases,
     * the bias must compensate for the accumulated evidence across phases.
     */
    public static final double DEFAULT_BIAS = -0.5;
    public static final String BIAS_CONFIG = "adaptive-algorithm-log-odds-bias";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Log-Odds Risk Algorithm";
    }

    @Override
    public String getDescription() {
        return "Bayesian evidence-based risk algorithm using log-odds. " +
                "Each signal contributes evidence (positive for risk, negative for trust) " +
                "which is aggregated and transformed via logistic function to produce risk probability.";
    }

    @Override
    public RiskScoreAlgorithm create(KeycloakSession session) {
        return new LogOddsRiskAlgorithm(
                session,
                DEFAULT_BIAS,
                LogOddsDefaultRiskLevels.simple(),
                LogOddsDefaultRiskLevels.advanced()
        );
    }

    @Override
    public String getHelpText() {
        return getDescription();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(BIAS_CONFIG)
                .label("Log odds bias")
                .helpText("Prior log-odds bias before any evidence is considered. " +
                        "Negative values lower baseline risk (e.g. -6.9 for ~0.1% fraud rate), " +
                        "positive values raise it (e.g. -2.2 for ~10% fraud rate). " +
                        "Default: " + DEFAULT_BIAS)
                .type(ProviderConfigProperty.NUMBER_TYPE)
                .defaultValue(DEFAULT_BIAS)
                .add()
                .build();
    }

    @Override
    public int order() {
        return 100;
    }
}
