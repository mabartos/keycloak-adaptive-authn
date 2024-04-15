package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.RiskLevel;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

import java.util.Set;

public class DefaultUserRoleEvaluator implements RiskFactorEvaluator {
    private final KeycloakSession session;
    private final UserRoleContext context;
    private Double risk;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
        this.context = ContextUtils.getContext(session, UserRoleContext.class, UserRoleContextFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return Set.of(context);
    }

    @Override
    public void evaluate() {
        // TODO
        if (context.getData().stream().map(RoleModel::getName).anyMatch(f -> f.equals("ADMIN"))) {
            risk = RiskLevel.INTERMEDIATE;
        } else {
            risk = RiskLevel.SMALL;
        }
    }
}
