package io.github.mabartos.spi.evaluator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link RiskEvaluator.EvaluationPhase} a risk evaluator targets.
 * Used by {@link RiskEvaluatorFactory#evaluationPhase()} to derive the phase from the evaluator class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EvaluationPhase {
    RiskEvaluator.EvaluationPhase value();
}
