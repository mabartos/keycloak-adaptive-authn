package io.github.mabartos.ui;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;

/**
 * Formats realm admin labels and tooltips from {@link RiskEvaluatorFactory} metadata.
 */
final class RiskEvaluatorUi {

    private static final String ENABLED_SUFFIX =
            " When disabled, this evaluator is skipped and does not contribute evidence to the risk score.";

    private static final String TRUST_SUFFIX =
            " Adjust how strongly this evaluator influences the combined score (log-odds algorithm).";

    private RiskEvaluatorUi() {
    }

    static String phaseLabelPrefix(RiskEvaluator.EvaluationPhase phase) {
        return switch (phase) {
            case BEFORE_AUTHN -> "BEFORE_AUTHN";
            case USER_KNOWN -> "USER_KNOWN";
            case CONTINUOUS -> "CONTINUOUS";
        };
    }

    static String enabledLabel(RiskEvaluatorFactory factory) {
        return "[" + phaseLabelPrefix(factory.evaluationPhase()) + "] " + factory.adminDisplayName();
    }

    static String trustLabel(RiskEvaluatorFactory factory) {
        return enabledLabel(factory) + " trust";
    }

    static String enabledTooltip(RiskEvaluatorFactory factory) {
        return factory.adminEnabledHelpText() + ENABLED_SUFFIX;
    }

    static String trustTooltip(RiskEvaluatorFactory factory) {
        return factory.adminTrustHelpText() + TRUST_SUFFIX;
    }
}
