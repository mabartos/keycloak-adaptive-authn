package io.github.mabartos.docs;

import io.github.mabartos.engine.algorithm.LogOddsDefaultRiskLevels;
import io.github.mabartos.engine.algorithm.LogOddsRiskAlgorithm;
import io.github.mabartos.engine.algorithm.LogOddsRiskAlgorithmFactory;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.RiskLevel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Log-odds algorithm constants for documentation tools. */
public final class LogOddsAlgorithmConstants {

    private static final LogOddsRiskAlgorithm.ValuesMapper VALUES_MAPPER = new LogOddsRiskAlgorithm.ValuesMapper();

    private static final Map<String, String> SCORE_LABELS = Map.ofEntries(
            Map.entry("NEGATIVE_HIGH", "Strong trust signal"),
            Map.entry("NEGATIVE_LOW", "Weak trust signal"),
            Map.entry("NONE", "Neutral"),
            Map.entry("VERY_SMALL", "Minimal risk"),
            Map.entry("SMALL", "Low risk"),
            Map.entry("MEDIUM", "Moderate risk"),
            Map.entry("HIGH", "High risk"),
            Map.entry("VERY_HIGH", "Very high risk"),
            Map.entry("EXTREME", "Extreme risk")
    );

    private LogOddsAlgorithmConstants() {
    }

    public record LevelThreshold(String name, double min, double max) {
        public static LevelThreshold from(RiskLevel level) {
            return new LevelThreshold(level.name(), level.lowestRiskValue(), level.highestRiskValue());
        }
    }

    public record ScoreEvidence(String score, double evidence, String label) {
    }

    public static double defaultBias() {
        return LogOddsRiskAlgorithmFactory.DEFAULT_BIAS;
    }

    public static List<ScoreEvidence> scoreEvidenceMapping() {
        return Arrays.stream(Risk.Score.values())
                .filter(score -> score != Risk.Score.INVALID)
                .map(score -> new ScoreEvidence(
                        score.name(),
                        VALUES_MAPPER.getRiskValue(Risk.of(score)).orElseThrow(),
                        SCORE_LABELS.get(score.name())
                ))
                .toList();
    }

    public static List<String> validScoreNames() {
        return scoreEvidenceMapping().stream()
                .map(ScoreEvidence::score)
                .toList();
    }

    public static Map<String, Double> evidenceByScoreName() {
        return scoreEvidenceMapping().stream()
                .collect(Collectors.toUnmodifiableMap(ScoreEvidence::score, ScoreEvidence::evidence));
    }

    public static List<LevelThreshold> simpleLevels() {
        return LogOddsDefaultRiskLevels.simple().getLevels().stream()
                .map(LevelThreshold::from)
                .toList();
    }

    public static List<LevelThreshold> advancedLevels() {
        return LogOddsDefaultRiskLevels.advanced().getLevels().stream()
                .map(LevelThreshold::from)
                .toList();
    }
}
