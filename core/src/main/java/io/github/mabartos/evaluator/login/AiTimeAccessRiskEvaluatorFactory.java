package io.github.mabartos.evaluator.login;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class AiTimeAccessRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "ai-time-access";
    public static final String NAME = "Check access time by leveraging AI";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return AiTimeAccessRiskEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new AiTimeAccessRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
