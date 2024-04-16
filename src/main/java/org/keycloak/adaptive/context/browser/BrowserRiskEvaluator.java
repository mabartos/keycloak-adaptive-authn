package org.keycloak.adaptive.context.browser;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.RiskLevel;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class BrowserRiskEvaluator implements RiskFactorEvaluator {
    private static final Logger logger = Logger.getLogger(BrowserRiskEvaluator.class);

    private final KeycloakSession session;
    private final BrowserCondition browserCondition;
    private Double risk;

    public BrowserRiskEvaluator(KeycloakSession session) {
        this.session = session;
        this.browserCondition = ContextUtils.getContextCondition(session, BrowserConditionFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        return risk;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return browserCondition.getUserContexts();
    }

    @Override
    public void evaluate() {
        var isKnown = browserCondition.isDefaultKnownBrowser();

        if (isKnown) {
            this.risk = RiskLevel.SMALL;
        } else {
            this.risk = RiskLevel.INTERMEDIATE;
        }
        logger.debugf("Risk for browser evaluated to: '%s'", risk);
    }
}
