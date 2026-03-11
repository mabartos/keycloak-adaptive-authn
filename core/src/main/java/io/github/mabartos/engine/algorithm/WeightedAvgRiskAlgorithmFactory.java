package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import org.keycloak.models.KeycloakSession;

public class WeightedAvgRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "weighted-average";
    private static RiskScoreAlgorithm SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Weighted average";
    }

    @Override
    public String getDescription() {
        return "Compute the overall risk score by leveraging weighted average algorithm";
    }

    @Override
    public RiskScoreAlgorithm create(KeycloakSession session) {
        if (SINGLETON == null) {
            SINGLETON = new WeightedAvgRiskAlgorithm();
        }
        return SINGLETON;
    }
}
