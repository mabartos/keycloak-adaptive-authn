package io.github.mabartos.engine.algorithm;

import io.github.mabartos.level.Trust;
import io.github.mabartos.spi.engine.RiskScoreAlgorithm;
import io.github.mabartos.spi.engine.StoredRiskProvider;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.level.AdvancedRiskLevels;
import io.github.mabartos.spi.level.ResultRisk;
import io.github.mabartos.spi.level.Risk;
import io.github.mabartos.spi.level.SimpleRiskLevels;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.of;

public class WeightedAvgRiskAlgorithm implements RiskScoreAlgorithm {
    private final ValuesMapper valuesMapper;
    private final StoredRiskProvider storedRiskProvider;
    private final SimpleRiskLevels simpleRiskLevels;
    private final AdvancedRiskLevels advancedRiskLevels;

    public WeightedAvgRiskAlgorithm(
            KeycloakSession session,
            SimpleRiskLevels simpleRiskLevels,
            AdvancedRiskLevels advancedRiskLevels
    ) {
        this.simpleRiskLevels = simpleRiskLevels;
        this.advancedRiskLevels = advancedRiskLevels;
        this.valuesMapper = new ValuesMapper();
        this.storedRiskProvider = session.getProvider(StoredRiskProvider.class);
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

        var weightedRisk = filteredEvaluators.stream()
                .filter(eval -> valuesMapper.isValid(eval.getRisk()))
                .mapToDouble(eval -> valuesMapper.getRiskValue(eval.getRisk()).get() * eval.getTrust(realm))
                .sum();

        var trustSum = filteredEvaluators.stream()
                .mapToDouble(f -> f.getTrust(realm))
                .sum();

        // Weighted arithmetic mean
        if (trustSum == 0) {
            return ResultRisk.invalid("No valid evaluators found for this phase");
        }
        var risk = ResultRisk.of(weightedRisk / trustSum);

        storedRiskProvider.storeRisk(risk, phase);

        return risk;
    }

    @Override
    public ResultRisk getOverallRisk() {
        var beforeAuthnRisk = storedRiskProvider.getStoredRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        var userKnownRisk = storedRiskProvider.getStoredRisk(RiskEvaluator.EvaluationPhase.USER_KNOWN);

        if (beforeAuthnRisk.isValid() && userKnownRisk.isValid()) {
            return beforeAuthnRisk.getScore() > userKnownRisk.getScore() ? beforeAuthnRisk : userKnownRisk;
        }

        if (beforeAuthnRisk.isValid()) {
            return beforeAuthnRisk;
        }

        return userKnownRisk;
    }

    public static class ValuesMapper implements RiskValuesMapper {
        @Override
        public Optional<Double> getRiskValue(Risk risk) {
            if (risk == null) {
                return Optional.empty();
            }

            return switch (risk.getScore()) {
                case INVALID -> Optional.empty();
                case NONE, NEGATIVE_HIGH, NEGATIVE_LOW -> of(0.0);
                case VERY_SMALL -> of(0.1);
                case SMALL -> of(0.3);
                case MEDIUM -> of(0.5);
                case HIGH -> of(0.7);
                case VERY_HIGH -> of(0.85);
                case EXTREME -> of(1.0);
            };
        }

        @Override
        public boolean isValid(Risk risk) {
            return getRiskValue(risk)
                    .filter(Trust::isValid)
                    .isPresent();
        }
    }
}
