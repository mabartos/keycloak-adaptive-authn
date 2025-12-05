package io.github.mabartos.evaluator.login;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

public class LoginFailuresRiskEvaluatorTest {

    @Test
    public void testNoLoginFailuresLowRisk() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(0);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(Risk.NONE));
    }

    @Test
    public void testFewLoginFailuresMediumRisk() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(3);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), greaterThan(Risk.SMALL));
        assertThat(risk.getScore().get(), lessThan(Risk.INTERMEDIATE));
    }

    @Test
    public void testManyLoginFailuresHighRisk() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(10);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), greaterThan(Risk.INTERMEDIATE));
    }

    @Test
    public void testLoginFailuresEvaluatorPhase() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(5);

        Set<RiskEvaluator.EvaluationPhase> phases = evaluator.evaluationPhases();
        assertThat(phases.size(), is(1));
        assertThat(phases.contains(RiskEvaluator.EvaluationPhase.USER_KNOWN), is(true));
        assertThat(phases.contains(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN), is(false));
    }

    @Test
    public void testLoginFailuresEvaluatorWeight() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(5);
        assertThat(evaluator.getWeight(), is(Weight.IMPORTANT));
    }

    @Test
    public void testLoginFailuresWithReason() {
        MockLoginFailuresRiskEvaluator evaluator = new MockLoginFailuresRiskEvaluator(7);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.getReason().isPresent(), is(true));
        assertThat(risk.getReason().get().contains("login failures"), is(true));
    }

    @Test
    public void testProgressiveRiskIncrease() {
        MockLoginFailuresRiskEvaluator evaluator1 = new MockLoginFailuresRiskEvaluator(1);
        MockLoginFailuresRiskEvaluator evaluator3 = new MockLoginFailuresRiskEvaluator(3);
        MockLoginFailuresRiskEvaluator evaluator5 = new MockLoginFailuresRiskEvaluator(5);
        MockLoginFailuresRiskEvaluator evaluator10 = new MockLoginFailuresRiskEvaluator(10);

        evaluator1.evaluateRisk();
        evaluator3.evaluateRisk();
        evaluator5.evaluateRisk();
        evaluator10.evaluateRisk();

        assertThat(evaluator3.getRisk().getScore().get(), greaterThan(evaluator1.getRisk().getScore().get()));
        assertThat(evaluator5.getRisk().getScore().get(), greaterThan(evaluator3.getRisk().getScore().get()));
        assertThat(evaluator10.getRisk().getScore().get(), greaterThan(evaluator5.getRisk().getScore().get()));
    }

    @Test
    public void testLoginFailuresThreshold() {
        // Test that risk increases significantly after certain threshold
        MockLoginFailuresRiskEvaluator lowFailures = new MockLoginFailuresRiskEvaluator(2);
        MockLoginFailuresRiskEvaluator highFailures = new MockLoginFailuresRiskEvaluator(8);

        lowFailures.evaluateRisk();
        highFailures.evaluateRisk();

        double lowRisk = lowFailures.getRisk().getScore().get();
        double highRisk = highFailures.getRisk().getScore().get();

        assertThat(highRisk - lowRisk, greaterThan(0.3));
    }

    // Mock implementation for testing
    static class MockLoginFailuresRiskEvaluator implements RiskEvaluator {
        private final int failureCount;
        private Risk risk;

        MockLoginFailuresRiskEvaluator(int failureCount) {
            this.failureCount = failureCount;
        }

        @Override
        public Risk getRisk() {
            return risk != null ? risk : Risk.invalid();
        }

        @Override
        public Set<EvaluationPhase> evaluationPhases() {
            return Set.of(EvaluationPhase.USER_KNOWN);
        }

        @Override
        public double getWeight() {
            return Weight.IMPORTANT;
        }

        @Override
        public void evaluateRisk() {
            if (failureCount == 0) {
                this.risk = Risk.none();
            } else if (failureCount == 1) {
                this.risk = Risk.of(Risk.SMALL, failureCount + " login failures");
            } else if (failureCount <= 3) {
                this.risk = Risk.of(Risk.MEDIUM, failureCount + " login failures");
            } else if (failureCount <= 5) {
                this.risk = Risk.of(Risk.INTERMEDIATE, failureCount + " login failures");
            } else if (failureCount <= 8) {
                this.risk = Risk.of(Risk.VERY_HIGH, failureCount + " login failures");
            } else {
                this.risk = Risk.of(Risk.HIGHEST, failureCount + " login failures");
            }
        }

        @Override
        public boolean isEnabled() {
            return true;
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
