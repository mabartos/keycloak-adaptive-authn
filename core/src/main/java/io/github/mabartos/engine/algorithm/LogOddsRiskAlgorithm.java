package io.github.mabartos.engine.algorithm;

import io.github.mabartos.level.ResultRisk;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.of;

/**
 * Log-odds risk algorithm based on Bayesian evidence model.
 * Each evaluator returns an evidence score that answers: "How much does this signal increase the probability of attack?"
 * Evidence scores are aggregated and transformed using logistic function to produce final risk probability.
 */
public class LogOddsRiskAlgorithm implements RiskScoreAlgorithm {
    private final ValuesMapper valuesMapper;

    /**
     * Bias term representing the prior log-odds before any evidence is considered.
     * Adjusts the baseline fraud probability:
     * - bias = 0.0 → 50% base probability (neutral)
     * - bias > 0 → higher base probability (assumes more fraud)
     * - bias < 0 → lower base probability (assumes less fraud)
     *
     * Example bias values for different organization types:
     * - Public services (0.1% fraud): bias ≈ -6.9
     * - E-commerce (1-2% fraud): bias ≈ -4.6 to -3.9
     * - Financial institutions (2-5% fraud): bias ≈ -3.9 to -2.9
     * - High-security systems (10% fraud): bias ≈ -2.2
     * - Neutral/unknown: bias = 0.0 (50% prior)
     *
     * Future: Should be configurable per client, as different clients may have
     * different fraud rates even within the same realm.
     */
    private final double biasScore;

    public LogOddsRiskAlgorithm() {
        this(0.0);
    }

    public LogOddsRiskAlgorithm(double biasScore) {
        this(new ValuesMapper(), biasScore);
    }

    public LogOddsRiskAlgorithm(ValuesMapper valuesMapper, double biasScore) {
        this.valuesMapper = valuesMapper;
        this.biasScore = biasScore;
    }

    @Override
    public ResultRisk evaluateRisk(@Nonnull Set<RiskEvaluator> evaluators,
                                   @Nonnull RiskEvaluator.EvaluationPhase phase,
                                   @Nonnull RealmModel realm,
                                   @Nullable UserModel knownUser) {
        var filteredEvaluators = evaluators.stream()
                .filter(eval -> eval.getRisk() != null)
                .filter(f -> isValidWeight(f.getWeight(realm)))
                .collect(Collectors.toSet());

        // Calculate weighted evidence sum
        var weightedEvidenceSum = filteredEvaluators.stream()
                .filter(eval -> valuesMapper.isValid(eval.getRisk()))
                .mapToDouble(eval -> valuesMapper.getRiskValue(eval.getRisk()).get() * eval.getWeight(realm))
                .sum();

        if (filteredEvaluators.isEmpty()) {
            return ResultRisk.invalid("No valid evaluators found for this phase");
        }

        // Apply logistic transformation: P(fraud) = 1 / (1 + exp(-(evidence + bias)))
        double totalEvidence = weightedEvidenceSum + biasScore;
        double riskProbability = logisticTransform(totalEvidence);

        return ResultRisk.of(riskProbability);
    }

    /**
     * Logistic function to transform evidence score to probability
     *
     * @param evidence the total evidence score (can be negative or positive)
     * @return probability in range [0, 1]
     */
    private double logisticTransform(double evidence) {
        return 1.0 / (1.0 + Math.exp(-evidence));
    }

    /**
     * Maps Risk scores to evidence values.
     * Positive values indicate risk signals, negative values indicate trust signals.
     */
    public static class ValuesMapper implements RiskValuesMapper {
        @Override
        public Optional<Double> getRiskValue(Risk risk) {
            if (risk == null) {
                return Optional.empty();
            }

            return switch (risk.getScore()) {
                case INVALID -> Optional.empty();
                case NEGATIVE_HIGH -> of(-2.5);  // Strong trust signal (e.g., known good device)
                case NEGATIVE_LOW -> of(-0.3);   // Weak trust signal (e.g., recognized device)
                case NONE -> of(0.0);            // Neutral evidence
                case VERY_SMALL -> of(0.1);      // Minimal risk evidence
                case SMALL -> of(0.4);           // Low risk evidence
                case MEDIUM -> of(0.8);          // Moderate risk evidence
                case HIGH -> of(1.5);            // High risk evidence
                case VERY_HIGH -> of(2.0);       // Very high risk evidence
                case EXTREME -> of(2.5);         // Extreme risk evidence (e.g., Tor IP)
            };
        }

        @Override
        public boolean isValid(Risk risk) {
            return getRiskValue(risk).isPresent();
        }
    }

    protected static boolean isValidWeight(double value) {
        return value >= 0.0d && value <= 1.0d;
    }
}
