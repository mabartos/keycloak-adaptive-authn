package io.github.mabartos.level;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrustTest {

    @Test
    public void testTrustConstants() {
        assertThat(Trust.MINIMAL, is(0.2));
        assertThat(Trust.LOW, is(0.5));
        assertThat(Trust.MODERATE, is(0.8));
        assertThat(Trust.FULL, is(1.0));
    }

    @Test
    public void testTrustOrdering() {
        assertThat(Trust.MINIMAL < Trust.LOW, is(true));
        assertThat(Trust.LOW < Trust.MODERATE, is(true));
        assertThat(Trust.MODERATE < Trust.FULL, is(true));
    }

    @Test
    public void testTrustValidRange() {
        assertThat(Trust.MINIMAL >= 0.0, is(true));
        assertThat(Trust.MINIMAL <= 1.0, is(true));

        assertThat(Trust.LOW >= 0.0, is(true));
        assertThat(Trust.LOW <= 1.0, is(true));

        assertThat(Trust.MODERATE >= 0.0, is(true));
        assertThat(Trust.MODERATE <= 1.0, is(true));

        assertThat(Trust.FULL >= 0.0, is(true));
        assertThat(Trust.FULL <= 1.0, is(true));
    }

    @Test
    public void testTrustDifferences() {
        double minimalToLow = Trust.LOW - Trust.MINIMAL;
        double lowToModerate = Trust.MODERATE - Trust.LOW;
        double moderateToFull = Trust.FULL - Trust.MODERATE;

        assertThat(minimalToLow, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(lowToModerate, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(moderateToFull, is(org.hamcrest.Matchers.closeTo(0.2, 0.001)));
    }

    @Test
    public void testIsValid() {
        // Valid values
        assertThat(Trust.isValid(0.0), is(true));
        assertThat(Trust.isValid(0.5), is(true));
        assertThat(Trust.isValid(1.0), is(true));
        assertThat(Trust.isValid(Trust.MINIMAL), is(true));
        assertThat(Trust.isValid(Trust.LOW), is(true));
        assertThat(Trust.isValid(Trust.MODERATE), is(true));
        assertThat(Trust.isValid(Trust.FULL), is(true));

        // Invalid values
        assertThat(Trust.isValid(-0.1), is(false));
        assertThat(Trust.isValid(-1.0), is(false));
        assertThat(Trust.isValid(1.1), is(false));
        assertThat(Trust.isValid(2.0), is(false));
        assertThat(Trust.isValid(Double.NaN), is(false));
        assertThat(Trust.isValid(Double.POSITIVE_INFINITY), is(false));
        assertThat(Trust.isValid(Double.NEGATIVE_INFINITY), is(false));
    }
}
