package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;

/**
 * Default simple and advanced risk level thresholds for {@link LogOddsRiskAlgorithmFactory}.
 *
 * <p>Single source of truth for factory defaults and documentation tooling.
 */
public final class LogOddsDefaultRiskLevels {

    private static final RiskLevel SIMPLE_LEVEL_LOW = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.50);
    private static final RiskLevel SIMPLE_LEVEL_MEDIUM = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.50, 0.85);
    private static final RiskLevel SIMPLE_LEVEL_HIGH = new RiskLevel(SimpleRiskLevels.HIGH, 0.85, 1.0);

    private static final RiskLevel ADV_LEVEL_LOW = new RiskLevel(AdvancedRiskLevels.LOW, 0.0, 0.35);
    private static final RiskLevel ADV_LEVEL_MILD = new RiskLevel(AdvancedRiskLevels.MILD, 0.35, 0.55);
    private static final RiskLevel ADV_LEVEL_MEDIUM = new RiskLevel(AdvancedRiskLevels.MEDIUM, 0.55, 0.75);
    private static final RiskLevel ADV_LEVEL_MODERATE = new RiskLevel(AdvancedRiskLevels.MODERATE, 0.75, 0.90);
    private static final RiskLevel ADV_LEVEL_HIGH = new RiskLevel(AdvancedRiskLevels.HIGH, 0.90, 1.0);

    private static final SimpleRiskLevels SIMPLE = new SimpleRiskLevels(
            SIMPLE_LEVEL_LOW, SIMPLE_LEVEL_MEDIUM, SIMPLE_LEVEL_HIGH);
    private static final AdvancedRiskLevels ADVANCED = new AdvancedRiskLevels(
            ADV_LEVEL_LOW, ADV_LEVEL_MILD, ADV_LEVEL_MEDIUM, ADV_LEVEL_MODERATE, ADV_LEVEL_HIGH);

    private LogOddsDefaultRiskLevels() {
    }

    public static SimpleRiskLevels simple() {
        return SIMPLE;
    }

    public static AdvancedRiskLevels advanced() {
        return ADVANCED;
    }
}
