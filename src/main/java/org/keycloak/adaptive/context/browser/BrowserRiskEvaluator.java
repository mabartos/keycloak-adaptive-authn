package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.RiskLevel;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class BrowserRiskEvaluator implements RiskFactorEvaluator {
    private final KeycloakSession session;
    private final BrowserCondition browserCondition;
    private Double riskValue;

    public BrowserRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.browserCondition = ContextUtils.getContextCondition(session, BrowserCondition.class, BrowserConditionFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return browserCondition.getUserContexts();
    }

    @Override
    public void evaluate() {
        var isKnown = browserCondition.isDefaultKnownBrowser();

        if (isKnown) {
            this.riskValue = RiskLevel.SMALL;
        } else {
            this.riskValue = RiskLevel.INTERMEDIATE;
        }
    }
}
