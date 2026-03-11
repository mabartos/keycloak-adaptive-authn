package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.RiskScoreAlgorithmFactory;
import org.keycloak.models.KeycloakSession;

public class LogOddsRiskAlgorithmFactory implements RiskScoreAlgorithmFactory {
    public static final String PROVIDER_ID = "log-odds";
    private static LogOddsRiskAlgorithm SINGLETON;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return "Log-Odds Risk Algorithm";
    }

    @Override
    public String getDescription() {
        return "Bayesian evidence-based risk algorithm using log-odds. " +
                "Each signal contributes evidence (positive for risk, negative for trust) " +
                "which is aggregated and transformed via logistic function to produce risk probability.";
    }

    @Override
    public RiskScoreAlgorithm create(KeycloakSession keycloakSession) {
        if (SINGLETON == null) {
            SINGLETON = new LogOddsRiskAlgorithm();
        }
        return SINGLETON;
    }
}
