package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class UserActionsRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "user-actions-continuous";
    protected static final String NAME = "User actions";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Scores bursts of sensitive account events (email change, password reset, credential changes, etc) in the continuous evaluation phase.";
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
}
