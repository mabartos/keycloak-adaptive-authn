package org.keycloak.adaptive.evaluator;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class LoginFailuresRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "login-failures-risk-evaluator";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new LoginFailuresRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
