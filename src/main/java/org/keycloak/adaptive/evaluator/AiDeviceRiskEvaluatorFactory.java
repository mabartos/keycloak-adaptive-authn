package org.keycloak.adaptive.evaluator;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class AiDeviceRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "ai-device-risk-evaluator";
    public static final String NAME = "AI Device";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new AiDeviceRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
