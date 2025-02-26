package org.keycloak.adaptive.spi.engine;

import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.provider.Provider;

import java.util.Set;

public interface RiskScoreAlgorithm extends Provider {

    String getName();

    String getDescription();

    Risk evaluateRisk(Set<RiskEvaluator> evaluators, RiskEvaluator.EvaluationPhase phase);
}
