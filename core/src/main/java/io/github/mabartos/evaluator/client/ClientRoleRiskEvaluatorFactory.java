package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class ClientRoleRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "default-client-role-risk-factor";
    public static final String NAME = "Client Role";

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new ClientRoleRiskEvaluator(session);
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
        return ClientRoleRiskEvaluator.class;
    }

    @Override
    public String adminDisplayName() {
        return "Client role";
    }

    @Override
    public String adminEnabledHelpText() {
        return "Scores OAuth client roles for the user. Per-client role weights can be set under Client → Risk-based settings.";
    }

    @Override
    public String adminTrustHelpText() {
        return "Falls back to built-in heuristics when no client role mapping is configured.";
    }
}
