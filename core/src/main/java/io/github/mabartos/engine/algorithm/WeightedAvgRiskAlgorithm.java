package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.RiskLevel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.mabartos.spi.level.SimpleRiskLevels;
import io.github.mabartos.spi.level.AdvancedRiskLevels;

import static java.util.Optional.of;

public class WeightedAvgRiskAlgorithm implements RiskScoreAlgorithm {
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

    // Cached instances - validated once on creation
    private static final SimpleRiskLevels SIMPLE_RISK_LEVELS = new SimpleRiskLevels(SIMPLE_LEVEL_LOW, SIMPLE_LEVEL_MEDIUM, SIMPLE_LEVEL_HIGH);
    private static final AdvancedRiskLevels ADVANCED_RISK_LEVELS = new AdvancedRiskLevels(ADV_LEVEL_LOW, ADV_LEVEL_MILD, ADV_LEVEL_MEDIUM, ADV_LEVEL_MODERATE, ADV_LEVEL_HIGH);

    private final ValuesMapper valuesMapper;

    public WeightedAvgRiskAlgorithm() {
        this.valuesMapper = new ValuesMapper();
    }

    @Override
    @Nonnull
    public SimpleRiskLevels getSimpleRiskLevels() {
        return SIMPLE_RISK_LEVELS;
    }

    @Override
    @Nonnull
    public AdvancedRiskLevels getAdvancedRiskLevels() {
        return ADVANCED_RISK_LEVELS;
    }

    @Override
    public ResultRisk evaluateRisk(@Nonnull Set<RiskEvaluator> evaluators,
                                   @Nonnull RiskEvaluator.EvaluationPhase phase,
                                   @Nonnull RealmModel realm,
                                   @Nullable UserModel knownUser) {
        var filteredEvaluators = evaluators.stream()
                .filter(eval -> eval.getRisk() != null)
                .filter(f -> isValue0to1(f.getWeight(realm)))
                .collect(Collectors.toSet());

        var weightedRisk = filteredEvaluators.stream()
                .filter(eval -> valuesMapper.isValid(eval.getRisk()))
                .mapToDouble(eval -> valuesMapper.getRiskValue(eval.getRisk()).get() * eval.getWeight(realm))
                .sum();

        var weights = filteredEvaluators.stream()
                .mapToDouble(f -> f.getWeight(realm))
                .sum();

        // Weighted arithmetic mean
        if (weights == 0) {
            return ResultRisk.invalid("No valid evaluators found for this phase");
        }
        return ResultRisk.of(weightedRisk / weights);
    }

    public static class ValuesMapper implements RiskValuesMapper {
        @Override
        public Optional<Double> getRiskValue(Risk risk) {
            if (risk == null) {
                return Optional.empty();
            }

            return switch (risk.getScore()) {
                case INVALID -> Optional.empty();
                case NONE, NEGATIVE_HIGH, NEGATIVE_LOW -> of(0.0);
                case VERY_SMALL -> of(0.1);
                case SMALL -> of(0.3);
                case MEDIUM -> of(0.5);
                case HIGH -> of(0.7);
                case VERY_HIGH -> of(0.85);
                case EXTREME -> of(1.0);
            };
        }

        @Override
        public boolean isValid(Risk risk) {
            return getRiskValue(risk)
                    .filter(WeightedAvgRiskAlgorithm::isValue0to1)
                    .isPresent();
        }
    }

    protected static boolean isValue0to1(double value) {
        return value >= 0.0d && value <= 1.0d;
    }
}
