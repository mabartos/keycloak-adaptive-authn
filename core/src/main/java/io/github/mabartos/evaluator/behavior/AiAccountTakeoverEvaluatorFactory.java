package io.github.mabartos.evaluator.behavior;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Factory for AI-based account takeover detection.
 * <p>
 * This evaluator uses AI to analyze complex behavioral patterns that may indicate
 * account compromise. Unlike simple rule-based checks, it can detect subtle anomalies
 * across multiple dimensions (timing, location, actions, device changes).
 * <p>
 * <strong>Note:</strong> Requires an AI engine to be configured. Adds latency and cost
 * but provides sophisticated pattern recognition beyond what deterministic rules can achieve.
 */
public class AiAccountTakeoverEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "ai-account-takeover";
    public static final String NAME = "AI-based Account Takeover Detection (analyzes complex behavioral patterns)";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends RiskEvaluator> evaluatorClass() {
        return AiAccountTakeoverEvaluator.class;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new AiAccountTakeoverEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
