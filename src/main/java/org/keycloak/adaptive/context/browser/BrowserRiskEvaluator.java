package org.keycloak.adaptive.context.browser;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.evaluator.EvaluatorUtils;
import org.keycloak.adaptive.level.Risk;
import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class BrowserRiskEvaluator implements RiskEvaluator {
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
    public double getWeight() {
        return EvaluatorUtils.getStoredEvaluatorWeight(session, BrowserRiskEvaluatorFactory.NAME);
    }

    @Override
    public boolean isEnabled() {
        return EvaluatorUtils.isEvaluatorEnabled(session, BrowserRiskEvaluatorFactory.NAME);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public Set<UserContext<?>> getUserContexts() {
        return browserCondition.getUserContexts();
    }

    @Override
    public void evaluate() {
        var isKnown = browserCondition.isDefaultKnownBrowser();

        if (isKnown) {
            this.risk = Risk.SMALL;
        } else {
            this.risk = Risk.INTERMEDIATE;
        }
    }
}
