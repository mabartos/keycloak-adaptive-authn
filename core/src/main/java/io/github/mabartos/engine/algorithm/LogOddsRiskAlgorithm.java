package io.github.mabartos.engine.algorithm;

import io.github.mabartos.level.Trust;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProperties;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mabartos.engine.algorithm.LogOddsRiskAlgorithmFactory.BIAS_CONFIG;
import static io.github.mabartos.engine.algorithm.LogOddsRiskAlgorithmFactory.DEFAULT_BIAS;

import static java.util.Optional.of;

/**
 * Log-odds risk algorithm based on Bayesian evidence model.
 * Each evaluator returns an evidence score that answers: "How much does this signal increase the probability of attack?"
 * Evidence scores are aggregated and transformed using logistic function to produce final risk probability.
 */
public class LogOddsRiskAlgorithm implements RiskScoreAlgorithm {
    private static final Logger logger = Logger.getLogger(LogOddsRiskAlgorithm.class);

    private final String id;
    private final ValuesMapper valuesMapper;
    private final SimpleRiskLevels simpleRiskLevels;
    private final AdvancedRiskLevels advancedRiskLevels;
    private final StoredRiskProvider storedRiskProvider;

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
    private final double defaultBias;

    public LogOddsRiskAlgorithm(
            KeycloakSession session,
            @Nonnull String id,
            double defaultBias,
            SimpleRiskLevels simpleRiskLevels,
            AdvancedRiskLevels advancedRiskLevels
    ) {
        this.id = id;
        this.defaultBias = defaultBias;
        this.simpleRiskLevels = simpleRiskLevels;
        this.advancedRiskLevels = advancedRiskLevels;
        this.valuesMapper = new ValuesMapper();
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
    }

    @Override
    @Nonnull
    public String getId() {
        return id;
    }

    private double getBias(@Nonnull RealmModel realm) {
        return Optional.ofNullable(realm.getAttribute(BIAS_CONFIG))
                .map(Double::parseDouble)
                .orElse(defaultBias);
    }

    @Override
    @Nonnull
    public SimpleRiskLevels getSimpleRiskLevels() {
        return simpleRiskLevels;
    }

    @Override
    @Nonnull
    public AdvancedRiskLevels getAdvancedRiskLevels() {
        return advancedRiskLevels;
    }

    @Override
    public ResultRisk evaluateRisk(@Nonnull Set<RiskEvaluator> evaluators,
                                   @Nonnull RiskEvaluator.EvaluationPhase phase,
                                   @Nonnull RealmModel realm,
                                   @Nullable UserModel knownUser) {
        var filteredEvaluators = evaluators.stream()
                .filter(eval -> eval.getRisk() != null)
                .filter(f -> Trust.isValid(f.getTrust(realm)))
                .collect(Collectors.toSet());

        if (filteredEvaluators.isEmpty()) {
            return ResultRisk.invalid("No valid evaluators found for this phase");
        }

        // Calculate trust-weighted evidence sum
        var trustWeightedEvidenceSum = filteredEvaluators.stream()
                .filter(eval -> valuesMapper.isValid(eval.getRisk()))
                .mapToDouble(eval -> valuesMapper.getRiskValue(eval.getRisk()).get() * eval.getTrust(realm))
                .sum();

        // Apply logistic transformation: P(fraud) = 1 / (1 + exp(-(evidence + bias)))
        double bias = getBias(realm);
        double totalEvidence = trustWeightedEvidenceSum + bias;
        double riskProbability = logisticTransform(totalEvidence);

        logger.debugf("Log-Odds evaluation (phase: %s) - evidence: %f, bias: %f, totalEvidence: %f, riskProbability: %f",
                phase, trustWeightedEvidenceSum, bias, totalEvidence, riskProbability);

        var risk = ResultRisk.of(riskProbability);

        storedRiskProvider.storeRisk(risk, phase);
        storedRiskProvider.storeAdditionalData(getTotalEvidenceProperty(phase), Double.toString(totalEvidence));
        storedRiskProvider.storeAdditionalData(getBiasProperty(phase), Double.toString(bias));

        return risk;
    }

    @Override
    public ResultRisk getOverallRisk() {
        var beforeAuthnEvidence = getStoredEvidence(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        var userKnownEvidence = getStoredEvidence(RiskEvaluator.EvaluationPhase.USER_KNOWN);

        if (beforeAuthnEvidence.isEmpty() && userKnownEvidence.isEmpty()) {
            return ResultRisk.invalid("No evidence available");
        }

        var totalEvidence = beforeAuthnEvidence.orElse(0.0) + userKnownEvidence.orElse(0.0);
        return ResultRisk.of(logisticTransform(totalEvidence));
    }

    private Optional<Double> getStoredEvidence(RiskEvaluator.EvaluationPhase phase) {
        return storedRiskProvider.getAdditionalData(getTotalEvidenceProperty(phase))
                .flatMap(value -> {
                    try {
                        return Optional.of(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }

    private String getTotalEvidenceProperty(RiskEvaluator.EvaluationPhase phase) {
        return StoredRiskProperties.getAlgorithmPrefix(phase) + "total-evidence";
    }

    private String getBiasProperty(RiskEvaluator.EvaluationPhase phase) {
        return StoredRiskProperties.getAlgorithmPrefix(phase) + "bias";
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
}
