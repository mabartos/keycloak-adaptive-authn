package io.github.mabartos.engine.algorithm;

import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.RiskLevel;
import io.github.mabartos.spi.level.SimpleRiskLevels;
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
    // Simple 3-level thresholds calibrated for log-odds
    private static final RiskLevel SIMPLE_LEVEL_LOW = new RiskLevel(SimpleRiskLevels.LOW, 0.0, 0.50);
    private static final RiskLevel SIMPLE_LEVEL_MEDIUM = new RiskLevel(SimpleRiskLevels.MEDIUM, 0.50, 0.85);
    private static final RiskLevel SIMPLE_LEVEL_HIGH = new RiskLevel(SimpleRiskLevels.HIGH, 0.85, 1.0);

    // Advanced 5-level thresholds calibrated for log-odds
    private static final RiskLevel ADV_LEVEL_LOW = new RiskLevel(AdvancedRiskLevels.LOW, 0.0, 0.35);
    private static final RiskLevel ADV_LEVEL_MILD = new RiskLevel(AdvancedRiskLevels.MILD, 0.35, 0.55);
    private static final RiskLevel ADV_LEVEL_MEDIUM = new RiskLevel(AdvancedRiskLevels.MEDIUM, 0.55, 0.75);
    private static final RiskLevel ADV_LEVEL_MODERATE = new RiskLevel(AdvancedRiskLevels.MODERATE, 0.75, 0.90);
    private static final RiskLevel ADV_LEVEL_HIGH = new RiskLevel(AdvancedRiskLevels.HIGH, 0.90, 1.0);

    // Cached instances - validated once on creation
    private static final SimpleRiskLevels SIMPLE_RISK_LEVELS = new SimpleRiskLevels(SIMPLE_LEVEL_LOW, SIMPLE_LEVEL_MEDIUM, SIMPLE_LEVEL_HIGH);
    private static final AdvancedRiskLevels ADVANCED_RISK_LEVELS = new AdvancedRiskLevels(ADV_LEVEL_LOW, ADV_LEVEL_MILD, ADV_LEVEL_MEDIUM, ADV_LEVEL_MODERATE, ADV_LEVEL_HIGH);

    /**
     * Default bias for the algorithm.
     * Negative bias makes the algorithm less aggressive, requiring more evidence to reach high risk levels.
     */
    private static final double DEFAULT_BIAS = -0.5;

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
        this(DEFAULT_BIAS);
    }

    public LogOddsRiskAlgorithm(double biasScore) {
        this(new ValuesMapper(), biasScore);
    }

    public LogOddsRiskAlgorithm(ValuesMapper valuesMapper, double biasScore) {
        this.valuesMapper = valuesMapper;
        this.biasScore = biasScore;
    }

    @Override
    @Nonnull
    public SimpleRiskLevels getSimpleRiskLevels() {
        return SIMPLE_RISK_LEVELS;
    }

    @Override
    @Nonnull
    public AdvancedRiskLevels getAdvancedRiskLevels() {
        return ADVANCED_RISK_LEVELS;
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
