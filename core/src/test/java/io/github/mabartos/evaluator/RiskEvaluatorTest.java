package io.github.mabartos.evaluator;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class RiskEvaluatorTest {

    @Test
    public void testBasicRiskEvaluator() {
        TestRiskEvaluator evaluator = new TestRiskEvaluator(0.6, Weight.NORMAL);

        assertThat(evaluator.isEnabled(), is(true));
        assertThat(evaluator.getWeight(), is(Weight.NORMAL));
        assertThat(evaluator.allowRetries(), is(true));

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk, notNullValue());
        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(0.6));
    }

    @Test
    public void testEvaluationPhases() {
        TestRiskEvaluator beforeAuthn = new TestRiskEvaluator(0.5, Weight.LOW,
            Set.of(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN));

        assertThat(beforeAuthn.evaluationPhases(), notNullValue());
        assertThat(beforeAuthn.evaluationPhases().size(), is(1));
        assertThat(beforeAuthn.evaluationPhases().contains(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN), is(true));

        TestRiskEvaluator multiPhase = new TestRiskEvaluator(0.7, Weight.IMPORTANT,
            Set.of(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, RiskEvaluator.EvaluationPhase.USER_KNOWN));

        assertThat(multiPhase.evaluationPhases().size(), is(2));
        assertThat(multiPhase.evaluationPhases(),
            containsInAnyOrder(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN, RiskEvaluator.EvaluationPhase.USER_KNOWN));
    }

    @Test
    public void testContinuousEvaluationPhase() {
        TestRiskEvaluator continuous = new TestRiskEvaluator(0.8, Weight.IMPORTANT,
            Set.of(RiskEvaluator.EvaluationPhase.CONTINUOUS));

        assertThat(continuous.evaluationPhases().contains(RiskEvaluator.EvaluationPhase.CONTINUOUS), is(true));
    }

    @Test
    public void testDisabledEvaluator() {
        DisabledRiskEvaluator evaluator = new DisabledRiskEvaluator();

        assertThat(evaluator.isEnabled(), is(false));

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk, notNullValue());
        assertThat(risk.isValid(), is(false));
    }

    @Test
    public void testEvaluatorWithReason() {
        TestRiskEvaluator evaluator = new TestRiskEvaluator(0.9, Weight.IMPORTANT);
        evaluator.setRiskReason("Suspicious IP address");

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.getReason().isPresent(), is(true));
        assertThat(risk.getReason().get(), is("Suspicious IP address"));
    }

    @Test
    public void testEvaluatorWeights() {
        TestRiskEvaluator negligible = new TestRiskEvaluator(0.5, Weight.NEGLIGIBLE);
        TestRiskEvaluator low = new TestRiskEvaluator(0.5, Weight.LOW);
        TestRiskEvaluator normal = new TestRiskEvaluator(0.5, Weight.NORMAL);
        TestRiskEvaluator important = new TestRiskEvaluator(0.5, Weight.IMPORTANT);

        assertThat(negligible.getWeight(), is(0.2));
        assertThat(low.getWeight(), is(0.5));
        assertThat(normal.getWeight(), is(0.8));
        assertThat(important.getWeight(), is(1.0));
    }

    @Test
    public void testNoRetriesEvaluator() {
        NoRetryRiskEvaluator evaluator = new NoRetryRiskEvaluator();

        assertThat(evaluator.allowRetries(), is(false));
    }

    @Test
    public void testAllEvaluationPhases() {
        Set<RiskEvaluator.EvaluationPhase> allPhases = Set.of(
            RiskEvaluator.EvaluationPhase.BEFORE_AUTHN,
            RiskEvaluator.EvaluationPhase.USER_KNOWN,
            RiskEvaluator.EvaluationPhase.CONTINUOUS
        );

        TestRiskEvaluator evaluator = new TestRiskEvaluator(0.5, Weight.NORMAL, allPhases);

        assertThat(evaluator.evaluationPhases().size(), is(3));
        assertThat(evaluator.evaluationPhases(), containsInAnyOrder(
            RiskEvaluator.EvaluationPhase.BEFORE_AUTHN,
            RiskEvaluator.EvaluationPhase.USER_KNOWN,
            RiskEvaluator.EvaluationPhase.CONTINUOUS
        ));
    }

    // Test implementations
    static class TestRiskEvaluator implements RiskEvaluator {
        private final double riskScore;
        private final double weight;
        private final Set<EvaluationPhase> phases;
        private Risk risk;
        private String riskReason;
        private boolean enabled = true;

        TestRiskEvaluator(double riskScore, double weight) {
            this(riskScore, weight, Set.of(EvaluationPhase.BEFORE_AUTHN));
        }

        TestRiskEvaluator(double riskScore, double weight, Set<EvaluationPhase> phases) {
            this.riskScore = riskScore;
            this.weight = weight;
            this.phases = phases;
        }

        public void setRiskReason(String reason) {
            this.riskReason = reason;
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
                this.risk = riskReason != null ? Risk.of(riskScore, riskReason) : Risk.of(riskScore);
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

    static class DisabledRiskEvaluator implements RiskEvaluator {
        @Override
        public Risk getRisk() {
            return Risk.invalid();
        }

        @Override
        public Set<EvaluationPhase> evaluationPhases() {
            return Set.of(EvaluationPhase.BEFORE_AUTHN);
        }

        @Override
        public double getWeight() {
            return Weight.NORMAL;
        }

        @Override
        public void evaluateRisk() {
            // No-op for disabled evaluator
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean allowRetries() {
            return true;
        }

        @Override
        public void close() {
        }
    }

    static class NoRetryRiskEvaluator implements RiskEvaluator {
        @Override
        public Risk getRisk() {
            return Risk.of(0.5);
        }

        @Override
        public Set<EvaluationPhase> evaluationPhases() {
            return Set.of(EvaluationPhase.USER_KNOWN);
        }

        @Override
        public double getWeight() {
            return Weight.NORMAL;
        }

        @Override
        public void evaluateRisk() {
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
}
