package org.keycloak.adaptive.context.agent;

import org.keycloak.adaptive.RiskConfidence;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Set;

public class UserAgentEvaluator implements RiskFactorEvaluator<DeviceContext> {
    private final KeycloakSession session;
    private final DeviceContext userAgentContext;
    private Double riskValue;

    public UserAgentEvaluator(KeycloakSession session) {
        this.session = session;
        this.userAgentContext = (DeviceContext) session.getProvider(UserContext.class, DeviceContextFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Collection<DeviceContext> getUserContexts() {
        return Set.of(userAgentContext);
    }

    @Override
    public void evaluate() {
        var agents = String.join(",", DefaultUserAgents.KNOWN_AGENTS);
        var anyOfKnownAgents = UserAgentConditionFactory.RULE_ANY_OF.match(userAgentContext, agents);
        if (anyOfKnownAgents) {
            this.riskValue = RiskConfidence.VERY_CONFIDENT;
        } else {
            this.riskValue = RiskConfidence.SMALL;
        }
    }
}
