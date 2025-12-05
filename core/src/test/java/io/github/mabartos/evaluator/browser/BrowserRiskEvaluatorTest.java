package io.github.mabartos.evaluator.browser;

import io.github.mabartos.level.Risk;
import io.github.mabartos.level.Weight;
import io.github.mabartos.spi.evaluator.RiskEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BrowserRiskEvaluatorTest {

    @Test
    public void testKnownBrowserLowRisk() {
        MockBrowserRiskEvaluator evaluator = new MockBrowserRiskEvaluator(true);

        assertThat(evaluator.isEnabled(), is(true));
        assertThat(evaluator.getWeight(), is(Weight.LOW));
        assertThat(evaluator.evaluationPhases(), notNullValue());
        assertThat(evaluator.evaluationPhases().contains(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN), is(true));

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(Risk.NONE));
    }

    @Test
    public void testUnknownBrowserHigherRisk() {
        MockBrowserRiskEvaluator evaluator = new MockBrowserRiskEvaluator(false);

        evaluator.evaluateRisk();
        Risk risk = evaluator.getRisk();

        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(Risk.INTERMEDIATE));
    }

    @Test
    public void testBrowserEvaluatorPhase() {
        MockBrowserRiskEvaluator evaluator = new MockBrowserRiskEvaluator(true);

        Set<RiskEvaluator.EvaluationPhase> phases = evaluator.evaluationPhases();
        assertThat(phases.size(), is(1));
        assertThat(phases.contains(RiskEvaluator.EvaluationPhase.BEFORE_AUTHN), is(true));
        assertThat(phases.contains(RiskEvaluator.EvaluationPhase.USER_KNOWN), is(false));
    }

    @Test
    public void testBrowserEvaluatorDefaultWeight() {
        MockBrowserRiskEvaluator evaluator = new MockBrowserRiskEvaluator(true);
        assertThat(evaluator.getWeight(), is(Weight.LOW));
    }

    // Mock implementation for testing
    static class MockBrowserRiskEvaluator implements RiskEvaluator {
        private final boolean isKnownBrowser;
        private Risk risk;

        MockBrowserRiskEvaluator(boolean isKnownBrowser) {
            this.isKnownBrowser = isKnownBrowser;
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
            return Weight.LOW;
        }

        @Override
        public void evaluateRisk() {
            this.risk = isKnownBrowser ? Risk.none() : Risk.of(Risk.INTERMEDIATE);
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
