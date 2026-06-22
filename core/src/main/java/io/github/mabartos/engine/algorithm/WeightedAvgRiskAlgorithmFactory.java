package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import org.keycloak.models.KeycloakSession;

/**
 * @deprecated Use {@link LogOddsRiskAlgorithmFactory} instead, which provides more accurate
 * Bayesian evidence-based risk scoring with configurable bias.
 */
@Deprecated(forRemoval = true)
public class WeightedAvgRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "weighted-average";

    // Simple 3-level with equal divisions
    private static final RiskLevel SIMPLE_LEVEL_LOW = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.33);
    private static final RiskLevel SIMPLE_LEVEL_MEDIUM = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.33, 0.66);
    private static final RiskLevel SIMPLE_LEVEL_HIGH = new RiskLevel(SimpleRiskLevels.HIGH, 0.66, 1.0);

    // Advanced 5-level with equal divisions
    private static final RiskLevel ADV_LEVEL_LOW = new RiskLevel(AdvancedRiskLevels.LOW, 0.0, 0.2);
    private static final RiskLevel ADV_LEVEL_MILD = new RiskLevel(AdvancedRiskLevels.MILD, 0.2, 0.4);
    private static final RiskLevel ADV_LEVEL_MEDIUM = new RiskLevel(AdvancedRiskLevels.MEDIUM, 0.4, 0.6);
    private static final RiskLevel ADV_LEVEL_MODERATE = new RiskLevel(AdvancedRiskLevels.MODERATE, 0.6, 0.8);
    private static final RiskLevel ADV_LEVEL_HIGH = new RiskLevel(AdvancedRiskLevels.HIGH, 0.8, 1.0);

    private static final SimpleRiskLevels SIMPLE_RISK_LEVELS = new SimpleRiskLevels(SIMPLE_LEVEL_LOW, SIMPLE_LEVEL_MEDIUM, SIMPLE_LEVEL_HIGH);
    private static final AdvancedRiskLevels ADVANCED_RISK_LEVELS = new AdvancedRiskLevels(ADV_LEVEL_LOW, ADV_LEVEL_MILD, ADV_LEVEL_MEDIUM, ADV_LEVEL_MODERATE, ADV_LEVEL_HIGH);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Weighted average (Deprecated)";
    }

    @Override
    public String getDescription() {
        return "[Deprecated] Compute the overall risk score by leveraging weighted average algorithm. Use 'Log-Odds' instead.";
    }

    @Override
    public RiskScoreAlgorithm create(KeycloakSession session) {
        return new WeightedAvgRiskAlgorithm(session, SIMPLE_RISK_LEVELS, ADVANCED_RISK_LEVELS);
    }

    @Override
    public int order() {
        return 0;
    }
}
