package io.github.mabartos.evaluator.login;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Factory for the time pattern risk evaluator.
 * This evaluator uses circular statistics to detect unusual login times.
 */
public class TimePatternRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "time-pattern";
    public static final String NAME = "Detect unusual login times based on user's typical pattern";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return TimePatternRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new TimePatternRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String adminDisplayName() {
        return "Unusual login time";
    }

    @Override
    public RiskEvaluator.EvaluationPhase evaluationPhase() {
        return RiskEvaluator.EvaluationPhase.USER_KNOWN;
    }

    @Override
    public String adminEnabledHelpText() {
        return "Flags logins outside the user's typical time-of-day/weekday pattern (learned from history).";
    }

    @Override
    public String adminTrustHelpText() {
        return "Needs sufficient login history; new users may produce neutral scores until the profile stabilizes.";
    }
}
