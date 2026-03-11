package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import org.keycloak.models.KeycloakSession;

public class WeightedAvgRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "weighted-average";

    // Eager initialization - validation happens at class load time
    private static final RiskScoreAlgorithm SINGLETON = new WeightedAvgRiskAlgorithm();

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
        return SINGLETON;
    }

    @Override
    public int order() {
        return 0;
    }
}
