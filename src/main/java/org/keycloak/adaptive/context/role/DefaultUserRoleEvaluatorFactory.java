package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class DefaultUserRoleEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "default-user-role-risk-factor";
    public static final String NAME = "Role";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new DefaultUserRoleEvaluator(session);
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
