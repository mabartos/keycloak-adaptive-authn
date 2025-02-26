package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.engine.RiskScoreAlgorithm;
import org.keycloak.adaptive.spi.engine.RiskScoreAlgorithmFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class WeightedAvgRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "weighted-average";
    private static RiskScoreAlgorithm SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public RiskScoreAlgorithm create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new WeightedAvgRiskAlgorithm();
        }
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
