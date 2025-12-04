package io.github.mabartos.engine;

import org.jboss.logging.Logger;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;

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
    public Risk evaluateRisk(Set<RiskEvaluator> evaluators, RiskEvaluator.EvaluationPhase phase) {
        var filteredEvaluators = evaluators.stream()
                .peek(WeightedAvgRiskAlgorithm::printEvaluatorDetails)
                .filter(eval -> eval.getRisk() != null)
                .filter(f -> Risk.isValid(f.getWeight()))
                .filter(eval -> eval.getRisk() != Risk.none())
                .filter(eval -> eval.getRisk().getScore().isPresent())
                .collect(Collectors.toSet());

        var weightedRisk = filteredEvaluators.stream()
                .mapToDouble(eval -> eval.getRisk().getScore().get() * eval.getWeight())
                .sum();

        var weights = filteredEvaluators.stream()
                .mapToDouble(RiskEvaluator::getWeight)
                .sum();

        // Weighted arithmetic mean
        return Risk.of(weightedRisk / weights);
    }

    private static void printEvaluatorDetails(RiskEvaluator evaluator) {
        logger.debugf("Evaluator: %s - Risk score: '%s' (weight '%f')%s",
                evaluator.getClass().getSimpleName(),
                evaluator.getRisk().getScore().map(score -> String.format("%.2f", score)).orElse("N/A"),
                Risk.isValid(evaluator.getWeight()) ? evaluator.getWeight() : -1.0,
                evaluator.getRisk().getReason().map(reason -> String.format(" - Reason: %s", reason)).orElse(""));
    }

    @Override
    public void close() {

    }
}
