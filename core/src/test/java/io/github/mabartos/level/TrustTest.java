package io.github.mabartos.level;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrustTest {

    @Test
    public void testTrustConstants() {
        assertThat(Trust.NEGLIGIBLE, is(0.2));
        assertThat(Trust.LOW, is(0.5));
        assertThat(Trust.NORMAL, is(0.8));
        assertThat(Trust.IMPORTANT, is(1.0));
        assertThat(Trust.DEFAULT, is(Trust.NORMAL));
    }

    @Test
    public void testTrustOrdering() {
        assertThat(Trust.NEGLIGIBLE < Trust.LOW, is(true));
        assertThat(Trust.LOW < Trust.NORMAL, is(true));
        assertThat(Trust.NORMAL < Trust.IMPORTANT, is(true));
    }

    @Test
    public void testTrustValidRange() {
        assertThat(Trust.NEGLIGIBLE >= 0.0, is(true));
        assertThat(Trust.NEGLIGIBLE <= 1.0, is(true));

        assertThat(Trust.LOW >= 0.0, is(true));
        assertThat(Trust.LOW <= 1.0, is(true));

        assertThat(Trust.NORMAL >= 0.0, is(true));
        assertThat(Trust.NORMAL <= 1.0, is(true));

        assertThat(Trust.IMPORTANT >= 0.0, is(true));
        assertThat(Trust.IMPORTANT <= 1.0, is(true));
    }

    @Test
    public void testDefaultTrust() {
        assertThat(Trust.DEFAULT, is(0.8));
        assertThat(Trust.DEFAULT, is(Trust.NORMAL));
    }

    @Test
    public void testTrustDifferences() {
        double negligibleToLow = Trust.LOW - Trust.NEGLIGIBLE;
        double lowToNormal = Trust.NORMAL - Trust.LOW;
        double normalToImportant = Trust.IMPORTANT - Trust.NORMAL;

        assertThat(negligibleToLow, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(lowToNormal, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(normalToImportant, is(org.hamcrest.Matchers.closeTo(0.2, 0.001)));
    }

    @Test
    public void testIsValid() {
        // Valid values
        assertThat(Trust.isValid(0.0), is(true));
        assertThat(Trust.isValid(0.5), is(true));
        assertThat(Trust.isValid(1.0), is(true));
        assertThat(Trust.isValid(Trust.NEGLIGIBLE), is(true));
        assertThat(Trust.isValid(Trust.LOW), is(true));
        assertThat(Trust.isValid(Trust.NORMAL), is(true));
        assertThat(Trust.isValid(Trust.IMPORTANT), is(true));
        assertThat(Trust.isValid(Trust.DEFAULT), is(true));

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
