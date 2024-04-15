package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class DefaultUserRoleEvaluatorFactory implements RiskFactorEvaluatorFactory {
    public static final String PROVIDER_ID = "default-user-role-risk-factor";

    @Override
    public RiskFactorEvaluator create(KeycloakSession session) {
        return new DefaultUserRoleEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
