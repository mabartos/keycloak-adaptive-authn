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
                .filter(f -> Risk.isValid(f.getWeight()))
                .filter(eval -> eval.getRisk() != null && eval.getRisk() != Risk.none())
                .filter(eval -> eval.getRisk().getScore().isPresent())
                .peek(eval -> logger.debugf("Evaluator: %s", eval.getClass().getSimpleName()))
                .peek(eval -> logger.debugf("Risk evaluated: %f (weight %f)", eval.getRisk().getScore().get(), eval.getWeight()))
                .mapToDouble(eval -> eval.getRisk().getScore().get() * eval.getWeight())
                .sum();

        var weights = evaluators.stream()
                .mapToDouble(RiskEvaluator::getWeight)
                .sum();

        // Weighted arithmetic mean
        return Risk.of(weightedRisk / weights);
    }

    @Override
    public void close() {

    }
}
