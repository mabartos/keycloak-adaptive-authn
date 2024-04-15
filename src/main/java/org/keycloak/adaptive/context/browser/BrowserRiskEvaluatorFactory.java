package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class BrowserRiskEvaluatorFactory implements RiskFactorEvaluatorFactory {

    public static final String PROVIDER_ID = "default-browser-risk-factor-evaluator";

    @Override
    public RiskFactorEvaluator create(KeycloakSession session) {
        return new BrowserRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
