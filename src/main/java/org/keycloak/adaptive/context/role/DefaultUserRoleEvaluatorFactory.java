package org.keycloak.adaptive.context.role;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultUserRoleEvaluatorFactory implements RiskFactorEvaluatorFactory<UserRoleContext> {
    public static final String PROVIDER_ID = "default-user-role-risk-factor";

    @Override
    public RiskFactorEvaluator<UserRoleContext> create(KeycloakSession session) {
        return new DefaultUserRoleEvaluator(session);
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
