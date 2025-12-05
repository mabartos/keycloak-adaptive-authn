package io.github.mabartos.evaluator.location;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class LocationRiskEvaluatorTest {

    @Test
    public void testSameCountryLowRisk() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", "US");

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(Risk.NONE));
    }

    @Test
    public void testDifferentCountryHigherRisk() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", "RU");

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), greaterThan(Risk.MEDIUM));
    }

    @Test
    public void testUnknownLocationMediumRisk() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", null);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(Risk.MEDIUM));
    }

    @Test
    public void testLocationEvaluatorPhase() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", "US");

        Set<RiskEvaluator.EvaluationPhase> phases = evaluator.evaluationPhases();
        assertThat(phases.size(), is(1));
        assertThat(phases.contains(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN), is(true));
    }

    @Test
    public void testLocationEvaluatorWeight() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", "US");
        assertThat(evaluator.getWeight(), is(Weight.NORMAL));
    }

    @Test
    public void testLocationEvaluatorWithReason() {
        MockLocationRiskEvaluator evaluator = new MockLocationRiskEvaluator("US", "CN");

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.getReason().isPresent(), is(true));
        assertThat(risk.getReason().get().contains("Different country"), is(true));
    }

    // Mock implementation for testing
    static class MockLocationRiskEvaluator implements RiskEvaluator {
        private final String expectedCountry;
        private final String actualCountry;
        private Risk risk;

        MockLocationRiskEvaluator(String expectedCountry, String actualCountry) {
            this.expectedCountry = expectedCountry;
            this.actualCountry = actualCountry;
        }

        @Override
        public Risk getRisk() {
            return risk != null ? risk : Risk.invalid();
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
            if (actualCountry == null) {
                this.risk = Risk.of(Risk.MEDIUM, "Unknown location");
            } else if (expectedCountry.equals(actualCountry)) {
                this.risk = Risk.none();
            } else {
                this.risk = Risk.of(Risk.VERY_HIGH, "Different country detected");
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
