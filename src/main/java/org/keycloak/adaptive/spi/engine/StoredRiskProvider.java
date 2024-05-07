package org.keycloak.adaptive.spi.engine;

import org.keycloak.provider.Provider;

import java.util.Optional;

public interface StoredRiskProvider extends Provider {

    Optional<Double> getStoredRisk();

    Optional<Double> getStoredRisk(RiskPhase riskPhase);

    void storeRisk(double risk);

    void storeRisk(double risk, RiskPhase riskPhase);

    enum RiskPhase {
        NO_USER,
        REQUIRES_USER,
        OVERALL
    }
}