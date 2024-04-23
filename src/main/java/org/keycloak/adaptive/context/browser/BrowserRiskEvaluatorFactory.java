package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class BrowserRiskEvaluatorFactory implements RiskEvaluatorFactory {

    public static final String PROVIDER_ID = "default-browser-risk-factor-evaluator";
    public static final String NAME = "Browser";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new BrowserRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public String getName() {
        return NAME;
    }
}
