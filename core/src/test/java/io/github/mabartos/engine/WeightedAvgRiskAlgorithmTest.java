package io.github.mabartos.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class WeightedAvgRiskAlgorithmTest {

    private WeightedAvgRiskAlgorithm algorithm;

    @BeforeEach
    public void setup() {
        algorithm = new WeightedAvgRiskAlgorithm();
    }

    @Test
    public void testGetName() {
        assertThat(algorithm.getName(), is("Weighted average"));
    }

    @Test
    public void testGetDescription() {
        assertThat(algorithm.getDescription(), notNullValue());
        assertThat(algorithm.getDescription().contains("weighted average"), is(true));
    }

    @Test
    public void testSingleEvaluator() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.5, 1.0));

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        assertThat(result.isValid(), is(true));
        assertThat(result.getScore().get(), is(0.5));
    }

    @Test
    public void testMultipleEvaluatorsEqualWeights() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.4, 1.0));
        evaluators.add(createMockEvaluator(0.6, 1.0));
        evaluators.add(createMockEvaluator(0.8, 1.0));

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.USER_KNOWN);

        assertThat(result.isValid(), is(true));
        // (0.4 + 0.6 + 0.8) / 3 = 0.6
        assertThat(result.getScore().get(), closeTo(0.6, 0.001));
    }

    @Test
    public void testMultipleEvaluatorsDifferentWeights() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.8, 0.5));  // Risk 0.8, Weight 0.5
        evaluators.add(createMockEvaluator(0.2, 1.0));  // Risk 0.2, Weight 1.0

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.USER_KNOWN);

        assertThat(result.isValid(), is(true));
        // (0.8 * 0.5 + 0.2 * 1.0) / (0.5 + 1.0) = (0.4 + 0.2) / 1.5 = 0.4
        assertThat(result.getScore().get(), closeTo(0.4, 0.001));
    }

    @Test
    public void testFilterInvalidWeights() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.5, 1.0));
        evaluators.add(createMockEvaluator(0.7, -0.5));  // Invalid weight, should be filtered
        evaluators.add(createMockEvaluator(0.9, 2.0));   // Invalid weight, should be filtered

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        assertThat(result.isValid(), is(true));
        // Only the first evaluator should be included
        assertThat(result.getScore().get(), is(0.5));
    }

    @Test
    public void testFilterNoneRisks() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.6, 1.0));
        evaluators.add(createMockEvaluator(0.0, 1.0));  // Risk 0.0 is included in weighted average

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        assertThat(result.isValid(), is(true));
        // Both evaluators should be included: (0.6 * 1.0 + 0.0 * 1.0) / 2.0 = 0.3
        assertThat(result.getScore().get(), is(0.3));
    }

    @Test
    public void testComplexWeightedScenario() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.3, 0.2));  // Low risk, low weight
        evaluators.add(createMockEvaluator(0.7, 0.5));  // Medium-high risk, medium weight
        evaluators.add(createMockEvaluator(0.9, 0.8));  // High risk, high weight

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.CONTINUOUS);

        assertThat(result.isValid(), is(true));
        // (0.3 * 0.2 + 0.7 * 0.5 + 0.9 * 0.8) / (0.2 + 0.5 + 0.8)
        // = (0.06 + 0.35 + 0.72) / 1.5 = 1.13 / 1.5 = 0.7533
        assertThat(result.getScore().get(), closeTo(0.7533, 0.001));
    }

    @Test
    public void testNullRiskHandling() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.5, 1.0));
        evaluators.add(createNullRiskEvaluator(1.0));  // Returns null risk

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.USER_KNOWN);

        assertThat(result.isValid(), is(true));
        // Null risk evaluator should be filtered out, only 0.5 counted
        assertThat(result.getScore().get(), is(0.5));
    }

    @Test
    public void testAllEvaluatorsFilteredOut() {
        Set<RiskEvaluator> evaluators = new HashSet<>();
        evaluators.add(createMockEvaluator(0.7, -0.5));  // Invalid weight
        evaluators.add(createMockEvaluator(0.9, 2.0));   // Invalid weight
        evaluators.add(createNullRiskEvaluator(1.0));    // Null risk

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.USER_KNOWN);

        // Should return invalid risk when no valid evaluators
        assertThat(result.isValid(), is(false));
    }

    @Test
    public void testEmptyEvaluatorSet() {
        Set<RiskEvaluator> evaluators = new HashSet<>();

        Risk result = algorithm.evaluateRisk(evaluators, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        // Should return invalid risk for empty set
        assertThat(result.isValid(), is(false));
    }

    // Helper methods to create mock evaluators
    private RiskEvaluator createMockEvaluator(double riskScore, double weight) {
        TestMockEvaluator evaluator = new TestMockEvaluator(riskScore, weight);
        evaluator.evaluateRisk();
        return evaluator;
    }

    static class TestMockEvaluator implements RiskEvaluator {
        private final double riskScore;
        private final double weight;
        private Risk risk;

        TestMockEvaluator(double riskScore, double weight) {
            this.riskScore = riskScore;
            this.weight = weight;
        }

        @Override
        public Risk getRisk() {
            return risk;
        }

        @Override
        public Set<EvaluationPhase> evaluationPhases() {
            return Set.of(EvaluationPhase.BEFORE_AUTHN);
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public void evaluateRisk() {
            this.risk = Risk.of(riskScore);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean allowRetries() {
            return false;
        }

        @Override
        public void close() {
        }
    }

    private RiskEvaluator createNullRiskEvaluator(double weight) {
        return new RiskEvaluator() {
            @Override
            public Risk getRisk() {
                return null;  // Intentionally return null
            }

            @Override
            public Set<EvaluationPhase> evaluationPhases() {
                return Set.of(EvaluationPhase.BEFORE_AUTHN);
            }

            @Override
            public double getWeight() {
                return weight;
            }

            @Override
            public void evaluateRisk() {
                // No-op
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean allowRetries() {
                return false;
            }

            @Override
            public void close() {
            }
        };
    }
}
