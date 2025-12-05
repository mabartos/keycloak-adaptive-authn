package io.github.mabartos.engine;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.engine.RiskEngine;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * Tests for RiskEngine interface contract and behavior.
 *
 * NOTE: This tests a simplified mock implementation to verify the basic contract.
 * For testing the real DefaultRiskEngine implementation with async processing,
 * transaction handling, and Keycloak infrastructure, see:
 * - BasicAdaptiveAuthnTest (integration test with real Keycloak)
 * - WeightedAvgRiskAlgorithmTest (unit test for the algorithm logic)
 */
public class RiskEngineTest {

    @Test
    public void testRiskEngineWithSingleEvaluator() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEvaluator(0.5, Weight.NORMAL, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        Risk overallRisk = engine.getOverallRisk();
        assertThat(overallRisk, notNullValue());
        assertThat(overallRisk.isValid(), is(true));
        assertThat(overallRisk.getScore().get(), is(0.5));
    }

    @Test
    public void testRiskEngineWithMultipleEvaluators() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEvaluator(0.3, Weight.LOW, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));
        engine.addEvaluator(createEvaluator(0.7, Weight.NORMAL, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));
        engine.addEvaluator(createEvaluator(0.9, Weight.IMPORTANT, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        Risk overallRisk = engine.getOverallRisk();
        assertThat(overallRisk.isValid(), is(true));
        // Weighted average: (0.3*0.5 + 0.7*0.8 + 0.9*1.0) / (0.5 + 0.8 + 1.0) = 1.61 / 2.3 ≈ 0.7
        assertThat(overallRisk.getScore().get(), closeTo(0.7, 0.01));
    }

    @Test
    public void testRiskEnginePhaseFiltering() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEvaluator(0.5, Weight.NORMAL, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));
        engine.addEvaluator(createEvaluator(0.7, Weight.NORMAL, RiskEvaluator.EvaluationPhase.USER_KNOWN));

        Set<RiskEvaluator> beforeAuthnEvaluators = engine.getRiskEvaluators(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        assertThat(beforeAuthnEvaluators.size(), is(1));

        Set<RiskEvaluator> userKnownEvaluators = engine.getRiskEvaluators(RiskEvaluator.EvaluationPhase.USER_KNOWN);
        assertThat(userKnownEvaluators.size(), is(1));
    }

    @Test
    public void testRiskEngineUserKnownPhase() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEvaluator(0.8, Weight.IMPORTANT, RiskEvaluator.EvaluationPhase.USER_KNOWN));

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.USER_KNOWN);

        Risk phaseRisk = engine.getRisk(RiskEvaluator.EvaluationPhase.USER_KNOWN);
        assertThat(phaseRisk.isValid(), is(true));
        assertThat(phaseRisk.getScore().get(), is(0.8));
    }

    @Test
    public void testRiskEngineDisabledEvaluator() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEnabledEvaluator(0.5, Weight.NORMAL, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, true));
        engine.addEvaluator(createEnabledEvaluator(0.9, Weight.IMPORTANT, RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, false));

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        Risk overallRisk = engine.getOverallRisk();
        assertThat(overallRisk.isValid(), is(true));
        // Only enabled evaluator should be considered
        assertThat(overallRisk.getScore().get(), is(0.5));
    }

    @Test
    public void testRiskEngineContinuousPhase() {
        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(createEvaluator(0.6, Weight.NORMAL, RiskEvaluator.EvaluationPhase.CONTINUOUS));

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.CONTINUOUS);

        Risk continuousRisk = engine.getRisk(RiskEvaluator.EvaluationPhase.CONTINUOUS);
        assertThat(continuousRisk.isValid(), is(true));
        assertThat(continuousRisk.getScore().get(), is(0.6));
    }

    @Test
    public void testRiskEngineNoEvaluators() {
        MockRiskEngine engine = new MockRiskEngine();

        engine.evaluateRisk(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);

        Risk overallRisk = engine.getOverallRisk();
        // Should return invalid when no evaluators
        assertThat(overallRisk.isValid(), is(false));
    }

    @Test
    public void testRiskEngineMultiPhaseEvaluator() {
        TestRiskEvaluator multiPhaseEvaluator = new TestRiskEvaluator(
            0.5,
            Weight.NORMAL,
            Set.of(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, RiskEvaluator.EvaluationPhase.USER_KNOWN)
        );

        MockRiskEngine engine = new MockRiskEngine();
        engine.addEvaluator(multiPhaseEvaluator);

        Set<RiskEvaluator> beforeAuthnEvaluators = engine.getRiskEvaluators(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN);
        assertThat(beforeAuthnEvaluators.size(), is(1));

        Set<RiskEvaluator> userKnownEvaluators = engine.getRiskEvaluators(RiskEvaluator.EvaluationPhase.USER_KNOWN);
        assertThat(userKnownEvaluators.size(), is(1));
    }

    // Helper methods
    private RiskEvaluator createEvaluator(double score, double weight, RiskEvaluator.EvaluationPhase phase) {
        return new TestRiskEvaluator(score, weight, Set.of(phase));
    }

    private RiskEvaluator createEnabledEvaluator(double score, double weight, RiskEvaluator.EvaluationPhase phase, boolean enabled) {
        return new TestRiskEvaluator(score, weight, Set.of(phase), enabled);
    }

    // Mock implementation of RiskEngine for testing
    static class MockRiskEngine implements RiskEngine {
        private final Set<RiskEvaluator> evaluators = new HashSet<>();
        private final WeightedAvgRiskAlgorithm algorithm = new WeightedAvgRiskAlgorithm();
        private Risk overallRisk = Risk.invalid();
        private Risk beforeAuthnRisk = Risk.invalid();
        private Risk userKnownRisk = Risk.invalid();
        private Risk continuousRisk = Risk.invalid();

        public void addEvaluator(RiskEvaluator evaluator) {
            evaluators.add(evaluator);
        }

        @Override
        public Risk getOverallRisk() {
            return overallRisk;
        }

        @Override
        public Risk getRisk(RiskEvaluator.EvaluationPhase phase) {
            return switch (phase) {
                case BEFORE_AUTHN -> beforeAuthnRisk;
                case USER_KNOWN -> userKnownRisk;
                case CONTINUOUS -> continuousRisk;
            };
        }

        @Override
        public Set<RiskEvaluator> getRiskEvaluators(RiskEvaluator.EvaluationPhase evaluationPhase) {
            return evaluators.stream()
                .filter(RiskEvaluator::isEnabled)
                .filter(e -> e.evaluationPhases().contains(evaluationPhase))
                .collect(java.util.stream.Collectors.toSet());
        }

        @Override
        public void evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase) {
            evaluateRisk(evaluationPhase, null, null);
        }

        @Override
        public void evaluateRisk(RiskEvaluator.EvaluationPhase evaluationPhase,
                                org.keycloak.models.RealmModel realm,
                                org.keycloak.models.UserModel knownUser) {
            Set<RiskEvaluator> phaseEvaluators = getRiskEvaluators(evaluationPhase);

            // Evaluate all evaluators
            phaseEvaluators.forEach(RiskEvaluator::evaluateRisk);

            // Calculate risk for this phase
            Risk phaseRisk = algorithm.evaluateRisk(phaseEvaluators, evaluationPhase);

            // Store phase-specific risk
            switch (evaluationPhase) {
                case BEFORE_AUTHN -> beforeAuthnRisk = phaseRisk;
                case USER_KNOWN -> userKnownRisk = phaseRisk;
                case CONTINUOUS -> continuousRisk = phaseRisk;
            }

            // Update overall risk (simplified - just use the latest phase risk)
            if (phaseRisk.isValid()) {
                overallRisk = phaseRisk;
            }
        }

        @Override
        public void close() {
        }
    }

    // Test implementation of RiskEvaluator
    static class TestRiskEvaluator implements RiskEvaluator {
        private final double riskScore;
        private final double weight;
        private final Set<EvaluationPhase> phases;
        private final boolean enabled;
        private Risk risk;

        TestRiskEvaluator(double riskScore, double weight, Set<EvaluationPhase> phases) {
            this(riskScore, weight, phases, true);
        }

        TestRiskEvaluator(double riskScore, double weight, Set<EvaluationPhase> phases, boolean enabled) {
            this.riskScore = riskScore;
            this.weight = weight;
            this.phases = phases;
            this.enabled = enabled;
        }

        @Override
        public Risk getRisk() {
            return risk != null ? risk : Risk.invalid();
        }

        @Override
        public Set<EvaluationPhase> evaluationPhases() {
            return phases;
        }

        @Override
        public double getWeight() {
            return weight;
        }

        @Override
        public void evaluateRisk() {
            if (enabled) {
                this.risk = Risk.of(riskScore);
            }
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean allowRetries() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
