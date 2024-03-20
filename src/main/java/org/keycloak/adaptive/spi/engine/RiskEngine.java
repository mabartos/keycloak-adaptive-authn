package org.keycloak.adaptive.spi.engine;

import org.keycloak.adaptive.spi.factor.RiskFactorEvaluator;
import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.provider.Provider;

import java.util.Set;

public interface RiskEngine extends Provider {

    Double getRiskValue();

    Set<UserContext> getRiskFactors();

    Set<RiskFactorEvaluator> getRiskEvaluators();

    void evaluateRisk();
}
