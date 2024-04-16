package org.keycloak.adaptive.engine;

import org.jboss.logging.Logger;
import org.keycloak.adaptive.context.browser.BrowserRiskEvaluator;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultRiskEngine implements RiskEngine {
    private static final Logger logger = Logger.getLogger(BrowserRiskEvaluator.class);

    private final KeycloakSession session;
    private final Set<RiskFactorEvaluator> riskFactorEvaluators;
    private Double riskValue;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.riskFactorEvaluators = session.getAllProviders(RiskFactorEvaluator.class);
    }

    @Override
    public void evaluateRisk() {
        logger.debugf("Risk Engine - EVALUATING");
        getRiskEvaluators().forEach(f -> {
            logger.debugf("Evaluator: %s", f.getClass().getSimpleName());
            f.evaluate();
            logger.debugf("Risk evaluated: %f", f.getRiskValue());
        });

        //todo very naive
        this.riskValue = getRiskEvaluators().stream().mapToDouble(RiskFactorEvaluator::getRiskValue).sum() / getRiskEvaluators().size();
        logger.debugf("The overall risk score is %f", riskValue);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Set<UserContext<?>> getRiskFactors() {
        return riskFactorEvaluators.stream()
                .flatMap(f -> f.getUserContexts().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<RiskFactorEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        evaluateRisk();
        storeRisk(context);
        context.success();
    }
}