package org.keycloak.adaptive.context.agent;

import org.keycloak.adaptive.RiskConfidence;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class UserAgentEvaluator implements RiskFactorEvaluator<UserAgentContext> {
    private final KeycloakSession session;
    private final UserAgentContext userAgentContext;
    private Double riskValue;

    public UserAgentEvaluator(KeycloakSession session) {
        this.session = session;
        this.userAgentContext = (UserAgentContext) session.getProvider(UserContext.class, HeaderUserAgentContextFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Collection<UserAgentContext> getUserContexts() {
        return Set.of(userAgentContext);
    }

    @Override
    public void evaluate() {
        var agents = DefaultUserAgents.KNOWN_AGENTS.stream().map(UserAgent::getName).collect(Collectors.joining(","));
        var anyOfKnownAgents = UserAgentConditionFactory.RULE_ANY_OF.match(userAgentContext, agents);
        if (anyOfKnownAgents) {
            this.riskValue = RiskConfidence.VERY_CONFIDENT;
        } else {
            this.riskValue = RiskConfidence.SMALL;
        }
    }
}
