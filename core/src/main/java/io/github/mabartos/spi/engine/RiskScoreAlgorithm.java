package io.github.mabartos.spi.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Algorithm used for evaluating overall risk score
 */
public interface RiskScoreAlgorithm extends Provider {

    /**
     * Get name of the algorithm
     */
    String getName();

    /**
     * Get description of the algorithm representing details about risk score calculation
     */
    String getDescription();

    /**
     * Evaluate complex risk score for certain evaluators
     *
     * @param evaluators risk evaluators
     * @param phase      evaluation phase
     * @return complex risk for a specific phase
     */
    Risk evaluateRisk(Set<RiskEvaluator> evaluators, RiskEvaluator.EvaluationPhase phase);
}
