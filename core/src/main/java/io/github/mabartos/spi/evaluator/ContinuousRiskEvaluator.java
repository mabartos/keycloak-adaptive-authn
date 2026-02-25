package io.github.mabartos.spi.evaluator;

import java.util.Set;

public abstract class ContinuousRiskEvaluator extends AbstractRiskEvaluator {

    @Override
    public Set<EvaluationPhase> evaluationPhases() {
        return Set.of(EvaluationPhase.CONTINUOUS);
    }
}
