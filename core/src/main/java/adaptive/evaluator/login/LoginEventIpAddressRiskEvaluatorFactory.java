package org.keycloak.adaptive.evaluator.login;

import org.keycloak.adaptive.spi.evaluator.RiskEvaluator;
import org.keycloak.adaptive.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class LoginEventIpAddressRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "login-event-ip-address-risk-evaluator";
    public static final String NAME = "Known IP Address risk evaluator";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new LoginEventIpAddressRiskEvaluator(session);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return LoginEventIpAddressRiskEvaluator.class;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
