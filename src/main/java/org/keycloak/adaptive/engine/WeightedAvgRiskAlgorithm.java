package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.engine.RiskScoreAlgorithm;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;

import java.util.Set;

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
        var weightedRisk = evaluators.stream()
                .filter(eval -> eval.getRisk() != null)
                .peek(WeightedAvgRiskAlgorithm::printEvaluatorDetails)
                .filter(f -> Risk.isValid(f.getWeight()))
                .filter(eval -> eval.getRisk() != Risk.none())
                .filter(eval -> eval.getRisk().getScore().isPresent())
                .mapToDouble(eval -> eval.getRisk().getScore().get() * eval.getWeight())
                .sum();

        var weights = evaluators.stream()
                .mapToDouble(RiskEvaluator::getWeight)
                .sum();

        // Weighted arithmetic mean
        return Risk.of(weightedRisk / weights);
    }

    private static void printEvaluatorDetails(RiskEvaluator evaluator) {
        logger.debugf("Evaluator: %s - Risk score: '%s' (weight '%f') %s",
                evaluator.getClass().getSimpleName(),
                evaluator.getRisk().getScore().orElse(-1.0),
                Risk.isValid(evaluator.getWeight()) ? evaluator.getWeight() : -1.0,
                evaluator.getRisk().getReason().orElse(""));
    }

    @Override
    public void close() {

    }
}
