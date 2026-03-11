package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.RiskLevelValidator;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for risk level validation to ensure algorithms cover the entire 0-1 spectrum.
 */
class RiskLevelValidationTest {

    @Test
    void testValidSimpleLevels() {
        // Valid configuration - should not throw
        List<RiskLevel> levels = List.of(
                new RiskLevel("LOW", 0.0, 0.33),
                new RiskLevel("MEDIUM", 0.33, 0.66),
                new RiskLevel("HIGH", 0.66, 1.0)
        );

        assertDoesNotThrow(() -> RiskLevelValidator.validate(levels, "TestSimpleLevels"));
    }

    @Test
    void testGapInLevels() {
        // Gap between 0.33 and 0.35
        List<RiskLevel> levels = List.of(
                new RiskLevel("LOW", 0.0, 0.33),
                new RiskLevel("MEDIUM", 0.35, 0.66),  // GAP!
                new RiskLevel("HIGH", 0.66, 1.0)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> RiskLevelValidator.validate(levels, "TestGapLevels"));

        assertTrue(exception.getMessage().contains("Gap detected"));
        assertTrue(exception.getMessage().contains("0.33-0.35"));
    }

    @Test
    void testOverlapInLevels() {
        // Overlap between 0.33 and 0.32
        List<RiskLevel> levels = List.of(
                new RiskLevel("LOW", 0.0, 0.33),
                new RiskLevel("MEDIUM", 0.32, 0.66),  // OVERLAP!
                new RiskLevel("HIGH", 0.66, 1.0)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> RiskLevelValidator.validate(levels, "TestOverlapLevels"));

        assertTrue(exception.getMessage().contains("Overlap detected"));
    }

    @Test
    void testDoesNotStartAtZero() {
        List<RiskLevel> levels = List.of(
                new RiskLevel("LOW", 0.1, 0.33),  // Doesn't start at 0.0!
                new RiskLevel("MEDIUM", 0.33, 0.66),
                new RiskLevel("HIGH", 0.66, 1.0)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> RiskLevelValidator.validate(levels, "TestStartAtZero"));

        assertTrue(exception.getMessage().contains("must start at 0.0"));
    }

    @Test
    void testDoesNotEndAtOne() {
        List<RiskLevel> levels = List.of(
                new RiskLevel("LOW", 0.0, 0.33),
                new RiskLevel("MEDIUM", 0.33, 0.66),
                new RiskLevel("HIGH", 0.66, 0.95)  // Doesn't end at 1.0!
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> RiskLevelValidator.validate(levels, "TestEndAtOne"));

        assertTrue(exception.getMessage().contains("must end at 1.0"));
    }

    @Test
    void testLogOddsRiskAlgorithmValidation() {
        // The actual LogOdds algorithm should pass validation
        // Validation happens in constructors when SimpleRiskLevels/AdvancedRiskLevels are created
        LogOddsRiskAlgorithm algorithm = new LogOddsRiskAlgorithm();
        assertDoesNotThrow(() -> algorithm.getSimpleRiskLevels());
        assertDoesNotThrow(() -> algorithm.getAdvancedRiskLevels());
    }

    @Test
    void testWeightedAvgRiskAlgorithmValidation() {
        // The actual WeightedAvg algorithm should pass validation
        // Validation happens in constructors when SimpleRiskLevels/AdvancedRiskLevels are created
        WeightedAvgRiskAlgorithm algorithm = new WeightedAvgRiskAlgorithm();
        assertDoesNotThrow(() -> algorithm.getSimpleRiskLevels());
        assertDoesNotThrow(() -> algorithm.getAdvancedRiskLevels());
    }

    @Test
    void testSimpleRiskLevelsWrapperValidation() {
        // Valid simple risk levels - should not throw
        RiskLevel low = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.33);
        RiskLevel medium = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.33, 0.66);
        RiskLevel high = new RiskLevel(SimpleRiskLevels.HIGH, 0.66, 1.0);

        assertDoesNotThrow(() -> new SimpleRiskLevels(low, medium, high));
    }

    @Test
    void testAdvancedRiskLevelsWrapperValidation() {
        // Valid advanced risk levels - should not throw
        RiskLevel low = new RiskLevel(AdvancedRiskLevels.LOW, 0.0, 0.2);
        RiskLevel mild = new RiskLevel(AdvancedRiskLevels.MILD, 0.2, 0.4);
        RiskLevel medium = new RiskLevel(AdvancedRiskLevels.MEDIUM, 0.4, 0.6);
        RiskLevel moderate = new RiskLevel(AdvancedRiskLevels.MODERATE, 0.6, 0.8);
        RiskLevel high = new RiskLevel(AdvancedRiskLevels.HIGH, 0.8, 1.0);

        assertDoesNotThrow(() -> new AdvancedRiskLevels(low, mild, medium, moderate, high));
    }

    @Test
    void testSimpleRiskLevelsWrapperFailsOnInvalidLevels() {
        // Invalid simple risk levels - gap between levels
        RiskLevel low = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.33);
        RiskLevel medium = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.35, 0.66);  // GAP!
        RiskLevel high = new RiskLevel(SimpleRiskLevels.HIGH, 0.66, 1.0);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new SimpleRiskLevels(low, medium, high));

        assertTrue(exception.getMessage().contains("Gap detected"));
    }
}
