package io.github.mabartos.ui;

import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;

/**
 * Formats realm admin labels and tooltips from {@link RiskEvaluatorFactory} metadata.
 */
final class RiskEvaluatorUi {

    private static final String ENABLED_SUFFIX =
            " When disabled, this evaluator is skipped and does not contribute evidence to the risk score.";

    private static final String TRUST_HELP =
            "Adjust how strongly this evaluator influences the combined score (log-odds algorithm).";

    private RiskEvaluatorUi() {
    }

    static String enabledLabel(RiskEvaluatorFactory factory) {
        return "[" + factory.evaluationPhase().name() + "] " + factory.getName();
    }

    static String trustLabel(RiskEvaluatorFactory factory) {
        return enabledLabel(factory) + " trust";
    }

    static String enabledTooltip(RiskEvaluatorFactory factory) {
        return factory.getDescription() + ENABLED_SUFFIX;
    }

    static String trustTooltip() {
        return TRUST_HELP;
    }
}
