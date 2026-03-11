package io.github.mabartos.spi.engine;

import io.github.mabartos.level.ResultRisk;
import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Optional;
import java.util.Set;

/**
 * Algorithm used for evaluating overall risk score
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

    interface RiskValuesMapper {
        Optional<Double> getRiskValue(Risk risk);

        boolean isValid(Risk risk);
    }

    @Override
    default void close() {
    }
}
