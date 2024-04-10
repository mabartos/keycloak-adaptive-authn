package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.RiskConfidence;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;
import java.util.Set;

public class BrowserRiskEvaluator implements RiskFactorEvaluator<DeviceContext> {
    private final KeycloakSession session;
    private final DeviceContext userAgentContext;
    private Double riskValue;

    public BrowserRiskEvaluator(KeycloakSession session) {
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
        var agents = String.join(",", DefaultBrowsers.DEFAULT_BROWSERS);
        /*var anyOfKnownAgents = BrowserConditionFactory.RULE_ANY_OF.match(userAgentContext, agents);
        if (anyOfKnownAgents) {
            this.riskValue = RiskConfidence.VERY_CONFIDENT;
        } else {
            this.riskValue = RiskConfidence.SMALL;
        }*/
    }
}
