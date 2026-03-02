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
}
