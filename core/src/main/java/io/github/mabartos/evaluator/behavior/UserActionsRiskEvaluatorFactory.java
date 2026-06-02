package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class UserActionsRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "user-actions-continuous";
    protected static final String NAME = "User actions continuous evaluator";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return UserActionsRiskEvaluator.class;
    }

    @Override
    public UserActionsRiskEvaluator create(KeycloakSession session) {
        return new UserActionsRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String adminDisplayName() {
        return "User actions";
    }

    @Override
    public RiskEvaluator.EvaluationPhase evaluationPhase() {
        return RiskEvaluator.EvaluationPhase.CONTINUOUS;
    }

    @Override
    public String adminEnabledHelpText() {
        return "Re-evaluates risk during the session when user actions occur (continuous phase, event-driven).";
    }

    @Override
    public String adminTrustHelpText() {
        return "Works with the adaptive event listener; not part of the initial login risk calculation only.";
    }
}
