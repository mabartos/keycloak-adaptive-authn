package io.github.mabartos.engine;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;
import java.util.stream.Collectors;

public class WeightedAvgRiskAlgorithm implements RiskScoreAlgorithm {
    private static final Logger logger = Logger.getLogger(WeightedAvgRiskAlgorithm.class);
    
    @Override
    public String getName() {
        return "Weighted average";
    }

    @Override
    public String getDescription() {
        return "Compute the overall risk score by leveraging weighted average algorithm";
    }

    @Override
    public Risk evaluateRisk(@Nonnull Set<RiskEvaluator> evaluators,
                             @Nonnull RiskEvaluator.EvaluationPhase phase,
                             @Nonnull RealmModel realm,
                             @Nullable UserModel knownUser) {
        var filteredEvaluators = evaluators.stream()
                .filter(eval -> eval.getRisk() != null)
                .filter(f -> Risk.isValid(f.getWeight(realm)))
                .filter(eval -> eval.getRisk().getScore().isPresent())
                .collect(Collectors.toSet());

        var weightedRisk = filteredEvaluators.stream()
                .mapToDouble(eval -> eval.getRisk().getScore().get() * eval.getWeight(realm))
                .sum();

        var weights = filteredEvaluators.stream()
                .mapToDouble(f -> f.getWeight(realm))
                .sum();

        // Weighted arithmetic mean
        if (weights == 0) {
            logger.warn("No valid evaluators found for phase: " + phase);
            return Risk.invalid("No valid evaluators found for this phase");
        }
        return Risk.of(weightedRisk / weights);
    }

    @Override
    public void close() {

    }
}
