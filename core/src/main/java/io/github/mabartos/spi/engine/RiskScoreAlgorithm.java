package io.github.mabartos.spi.engine;

import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Optional;
import java.util.Set;

/**
 * Algorithm used for evaluating overall risk score.
 * Each algorithm must provide its own risk level thresholds calibrated for how it
 * calculates and accumulates risk.
 */
public interface RiskScoreAlgorithm extends Provider {

    /**
     * Evaluate complex risk score for certain evaluators
     *
     * @param evaluators risk evaluators
     * @param phase      evaluation phase
     * @param realm      realm
     * @param knownUser  knownUser
     * @return complex risk for a specific phase
     */
    ResultRisk evaluateRisk(@Nonnull Set<RiskEvaluator> evaluators,
                            @Nonnull RiskEvaluator.EvaluationPhase phase,
                            @Nonnull RealmModel realm,
                            @Nullable UserModel knownUser);

    /**
     * Get simple risk levels (3 levels: LOW, MEDIUM, HIGH) calibrated for this algorithm.
     * Levels are validated on construction.
     *
     * @return simple risk levels
     */
    @Nonnull
    SimpleRiskLevels getSimpleRiskLevels();

    /**
     * Get advanced risk levels (5 levels: LOW, MILD, MEDIUM, MODERATE, HIGH) calibrated for this algorithm.
     * Levels are validated on construction.
     *
     * @return advanced risk levels
     */
    @Nonnull
    AdvancedRiskLevels getAdvancedRiskLevels();

    interface RiskValuesMapper {
        Optional<Double> getRiskValue(Risk risk);

        boolean isValid(Risk risk);
    }

    @Override
    default void close() {
    }
}
