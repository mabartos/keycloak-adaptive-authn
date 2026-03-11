package io.github.mabartos.engine.algorithm;

import io.github.mabartos.level.ResultRisk;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.of;

public class WeightedAvgRiskAlgorithm implements RiskScoreAlgorithm {
    private final ValuesMapper valuesMapper;

    public WeightedAvgRiskAlgorithm() {
        this.valuesMapper = new ValuesMapper();
    }

    @Override
    public String getName() {
        return "Weighted average";
    }

    @Override
    public String getDescription() {
        return "Compute the overall risk score by leveraging weighted average algorithm";
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
