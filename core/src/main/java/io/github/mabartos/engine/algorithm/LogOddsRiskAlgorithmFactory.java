package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
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

    // Simple 3-level thresholds calibrated for log-odds
    private static final RiskLevel SIMPLE_LEVEL_LOW = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.50);
    private static final RiskLevel SIMPLE_LEVEL_MEDIUM = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.50, 0.85);
    private static final RiskLevel SIMPLE_LEVEL_HIGH = new RiskLevel(SimpleRiskLevels.HIGH, 0.85, 1.0);

    // Advanced 5-level thresholds calibrated for log-odds
    private static final RiskLevel ADV_LEVEL_LOW = new RiskLevel(AdvancedRiskLevels.LOW, 0.0, 0.35);
    private static final RiskLevel ADV_LEVEL_MILD = new RiskLevel(AdvancedRiskLevels.MILD, 0.35, 0.55);
    private static final RiskLevel ADV_LEVEL_MEDIUM = new RiskLevel(AdvancedRiskLevels.MEDIUM, 0.55, 0.75);
    private static final RiskLevel ADV_LEVEL_MODERATE = new RiskLevel(AdvancedRiskLevels.MODERATE, 0.75, 0.90);
    private static final RiskLevel ADV_LEVEL_HIGH = new RiskLevel(AdvancedRiskLevels.HIGH, 0.90, 1.0);

    // Cached instances - validated once on creation
    private static final SimpleRiskLevels SIMPLE_RISK_LEVELS = new SimpleRiskLevels(SIMPLE_LEVEL_LOW, SIMPLE_LEVEL_MEDIUM, SIMPLE_LEVEL_HIGH);
    private static final AdvancedRiskLevels ADVANCED_RISK_LEVELS = new AdvancedRiskLevels(ADV_LEVEL_LOW, ADV_LEVEL_MILD, ADV_LEVEL_MEDIUM, ADV_LEVEL_MODERATE, ADV_LEVEL_HIGH);

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
        return new LogOddsRiskAlgorithm(session, DEFAULT_BIAS, SIMPLE_RISK_LEVELS, ADVANCED_RISK_LEVELS);
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
                .label("Log-Odds: Algorithm bias")
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
