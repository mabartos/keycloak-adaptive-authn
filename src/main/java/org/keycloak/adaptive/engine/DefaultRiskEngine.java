package org.keycloak.adaptive.engine;

import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class DefaultRiskEngine implements RiskEngine {
    private final KeycloakSession session;
    private final Set<UserContext> userContexts;
    private final Set<RiskFactorEvaluator> riskFactorEvaluators;
    private Double riskValue;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.userContexts = session.getAllProviders(UserContext.class);
        this.riskFactorEvaluators = session.getAllProviders(RiskFactorEvaluator.class);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Set<UserContext> getRiskFactors() {
        return userContexts;
    }

    @Override
    public Set<RiskFactorEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void evaluateRisk() {
        //todo very naive
        this.riskValue = getRiskEvaluators().stream().mapToDouble(RiskFactorEvaluator::getRiskValue).sum();
    }

    @Override
    public void close() {

    }
}
