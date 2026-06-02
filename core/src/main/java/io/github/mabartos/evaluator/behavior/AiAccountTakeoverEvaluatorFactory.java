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

    @Override
    public String adminDisplayName() {
        return "AI account takeover";
    }

    @Override
    public RiskEvaluator.EvaluationPhase evaluationPhase() {
        return RiskEvaluator.EvaluationPhase.USER_KNOWN;
    }

    @Override
    public String adminEnabledHelpText() {
        return "LLM behavioral analysis for account takeover after the user is known (experimental).";
    }

    @Override
    public String adminTrustHelpText() {
        return "Higher CPU/latency than rule-based evaluators; tune trust down while evaluating in non-production.";
    }
}
