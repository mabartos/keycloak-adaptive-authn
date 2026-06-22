package io.github.mabartos.evaluator.client;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class ClientRoleRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "default-client-role-risk-factor";
    public static final String NAME = "Client role";

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
    public String getDescription() {
        return "Scores risk from the user's roles on the requesting OAuth client using built-in prefix heuristics (manage-*, create-*, view-*, query-*, and selected admin roles).";
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return ClientRoleRiskEvaluator.class;   
    }
}
