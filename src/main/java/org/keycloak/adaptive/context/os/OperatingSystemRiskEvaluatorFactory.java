package org.keycloak.adaptive.context.os;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class OperatingSystemRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "default-operating-system-risk-evaluator-factory";
    public static final String NAME = "Operating System";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new OperatingSystemRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
