package org.keycloak.adaptive.context.role;

import org.keycloak.adaptive.RiskConfidence;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Set;

public class DefaultUserRoleEvaluator implements RiskFactorEvaluator<UserRoleContext> {
    private final KeycloakSession session;
    private final UserRoleContext context;
    private Double risk;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
        this.context = (UserRoleContext) session.getAllProviders(UserContext.class);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public Collection<UserRoleContext> getUserContexts() {
        return Set.of(context);
    }

    @Override
    public void evaluate() {
        //TODO
        risk = RiskConfidence.NONE;
    }
}
