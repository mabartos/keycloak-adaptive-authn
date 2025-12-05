package io.github.mabartos.level;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RiskTest {

    @Test
    public void testValidRiskScores() {
        Risk none = Risk.of(Risk.NONE);
        assertThat(none.isValid(), is(true));
        assertThat(none.getScore().isPresent(), is(true));
        assertThat(none.getScore().get(), is(0.0));

        Risk small = Risk.of(Risk.SMALL);
        assertThat(small.isValid(), is(true));
        assertThat(small.getScore().get(), is(0.3));

        Risk medium = Risk.of(Risk.MEDIUM);
        assertThat(medium.isValid(), is(true));
        assertThat(medium.getScore().get(), is(0.5));

        Risk highest = Risk.of(Risk.HIGHEST);
        assertThat(highest.isValid(), is(true));
        assertThat(highest.getScore().get(), is(1.0));
    }

    @Test
    public void testInvalidRiskScores() {
        Risk negative = Risk.of(-0.5);
        assertThat(negative.isValid(), is(false));
        assertThat(negative.getScore().isPresent(), is(false));

        Risk tooHigh = Risk.of(1.5);
        assertThat(tooHigh.isValid(), is(false));
        assertThat(tooHigh.getScore().isPresent(), is(false));

        Risk invalid = Risk.invalid();
        assertThat(invalid.isValid(), is(false));
        assertThat(invalid.getScore().isPresent(), is(false));
    }

    @Test
    public void testRiskWithReason() {
        Risk risk = Risk.of(0.7, "Unusual login location");
        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore().get(), is(0.7));
        assertThat(risk.getReason().isPresent(), is(true));
        assertThat(risk.getReason().get(), is("Unusual login location"));
    }

    @Test
    public void testRiskWithoutReason() {
        Risk risk = Risk.of(0.5);
        assertThat(risk.isValid(), is(true));
        assertThat(risk.getReason().isPresent(), is(false));

        Risk riskEmptyReason = Risk.of(0.5, "");
        assertThat(riskEmptyReason.isValid(), is(true));
        assertThat(riskEmptyReason.getReason().isPresent(), is(false));
    }

    @Test
    public void testNoneFactory() {
        Risk none = Risk.none();
        assertThat(none, notNullValue());
        assertThat(none.isValid(), is(true));
        assertThat(none.getScore().get(), is(0.0));
    }

    @Test
    public void testInvalidFactory() {
        Risk invalid = Risk.invalid();
        assertThat(invalid, notNullValue());
        assertThat(invalid.isValid(), is(false));
    }

    @Test
    public void testIsValidMethod() {
        assertThat(Risk.isValid(0.0), is(true));
        assertThat(Risk.isValid(0.5), is(true));
        assertThat(Risk.isValid(1.0), is(true));
        assertThat(Risk.isValid(-0.1), is(false));
        assertThat(Risk.isValid(1.1), is(false));
    }

    @Test
    public void testBoundaryValues() {
        Risk zero = Risk.of(0.0);
        assertThat(zero.isValid(), is(true));
        assertThat(zero.getScore().get(), is(0.0));

        Risk one = Risk.of(1.0);
        assertThat(one.isValid(), is(true));
        assertThat(one.getScore().get(), is(1.0));

        Risk justBelowZero = Risk.of(-0.0001);
        assertThat(justBelowZero.isValid(), is(false));

        Risk justAboveOne = Risk.of(1.0001);
        assertThat(justAboveOne.isValid(), is(false));
    }

    @Test
    public void testPredefinedConstants() {
        assertThat(Risk.NONE, is(0.0));
        assertThat(Risk.SMALL, is(0.3));
        assertThat(Risk.MEDIUM, is(0.5));
        assertThat(Risk.INTERMEDIATE, is(0.7));
        assertThat(Risk.VERY_HIGH, is(0.85));
        assertThat(Risk.HIGHEST, is(1.0));
    }
}
