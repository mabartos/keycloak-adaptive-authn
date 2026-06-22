package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class ClientSensitivityRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "default-client-sensitivity-risk-factor";
    public static final String NAME = "Client Sensitivity";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new ClientSensitivityRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return ClientSensitivityRiskEvaluator.class;
    }

    @Override
    public String adminDisplayName() {
        return "Client sensitivity";
    }

    @Override
    public String adminEnabledHelpText() {
        return "Raises risk for OAuth clients marked with higher sensitivity (client attribute). Runs before the user is identified.";
    }

    @Override
    public String adminTrustHelpText() {
        return "Applies the client sensitivity score when the requesting client is known from the authorization request.";
    }
}
