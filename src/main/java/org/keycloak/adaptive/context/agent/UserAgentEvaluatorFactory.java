package org.keycloak.adaptive.context.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserAgentEvaluatorFactory implements RiskFactorEvaluatorFactory<DeviceContext> {

    public static final String PROVIDER_ID = "default-user-agent-risk-factor-evaluator";

    @Override
    public RiskFactorEvaluator<DeviceContext> create(KeycloakSession session) {
        return new UserAgentEvaluator(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
