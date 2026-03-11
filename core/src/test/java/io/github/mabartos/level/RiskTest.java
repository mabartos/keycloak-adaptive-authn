package io.github.mabartos.level;

import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;

import static io.github.mabartos.spi.level.Risk.Score.EXTREME;
import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.INVALID;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NONE;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_HIGH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RiskTest {

    @Test
    public void testValidRiskScores() {
        Risk none = Risk.of(NONE);
        assertThat(none.isValid(), is(true));
        assertThat(none.getScore(), is(NONE));

        Risk small = Risk.of(SMALL);
        assertThat(small.isValid(), is(true));
        assertThat(small.getScore(), is(SMALL));

        Risk medium = Risk.of(MEDIUM);
        assertThat(medium.isValid(), is(true));
        assertThat(medium.getScore(), is(MEDIUM));

        Risk extreme = Risk.of(EXTREME);
        assertThat(extreme.isValid(), is(true));
        assertThat(extreme.getScore(), is(EXTREME));
    }

    @Test
    public void testInvalidRiskScores() {
        Risk invalid = Risk.invalid("test");
        assertThat(invalid.isValid(), is(false));
        assertThat(invalid.getScore(), is(INVALID));

        Risk invalidWithReason = Risk.invalid("Test reason");
        assertThat(invalidWithReason.isValid(), is(false));
        assertThat(invalidWithReason.getScore(), is(INVALID));
    }

    @Test
    public void testRiskWithReason() {
        Risk risk = Risk.of(HIGH, "Unusual login location");
        assertThat(risk.isValid(), is(true));
        assertThat(risk.getScore(), is(HIGH));
        assertThat(risk.getReason().isPresent(), is(true));
        assertThat(risk.getReason().get(), is("Unusual login location"));
    }

    @Test
    public void testRiskWithoutReason() {
        Risk risk = Risk.of(MEDIUM);
        assertThat(risk.isValid(), is(true));
        assertThat(risk.getReason().isPresent(), is(false));

        Risk riskEmptyReason = Risk.of(MEDIUM, "");
        assertThat(riskEmptyReason.isValid(), is(true));
        assertThat(riskEmptyReason.getReason().isPresent(), is(false));
    }

    @Test
    public void testInvalidFactory() {
        Risk invalid = Risk.invalid("test");
        assertThat(invalid, notNullValue());
        assertThat(invalid.isValid(), is(false));
    }

    @Test
    public void testScoreEnumOrdering() {
        assertThat(NONE.ordinal() < SMALL.ordinal(), is(true));
        assertThat(SMALL.ordinal() < MEDIUM.ordinal(), is(true));
        assertThat(MEDIUM.ordinal() < HIGH.ordinal(), is(true));
        assertThat(HIGH.ordinal() < VERY_HIGH.ordinal(), is(true));
        assertThat(VERY_HIGH.ordinal() < EXTREME.ordinal(), is(true));
    }

    @Test
    public void testMaxMethod() {
        Risk low = Risk.of(SMALL, "Low risk");
        Risk high = Risk.of(VERY_HIGH, "High risk");

        // Test with higher score - should return the higher one
        Risk result = low.max(high);
        assertThat(result.getScore(), is(VERY_HIGH));
        assertThat(result.getReason().get(), is("High risk"));

        // Test reversed order - should still return the higher one
        result = high.max(low);
        assertThat(result.getScore(), is(VERY_HIGH));

        // Test with null - should return this
        result = low.max(null);
        assertThat(result.getScore(), is(SMALL));

        // Test with invalid parameter - should return this
        result = low.max(Risk.invalid("test"));
        assertThat(result.getScore(), is(SMALL));

        // Test when this is invalid - should return the valid parameter
        Risk invalid = Risk.invalid("Invalid risk");
        result = invalid.max(high);
        assertThat(result.getScore(), is(VERY_HIGH));
        assertThat(result.getReason().get(), is("High risk"));

        // Test when both are invalid - should return this
        Risk invalid2 = Risk.invalid("Another invalid");
        result = invalid.max(invalid2);
        assertThat(result.getScore(), is(INVALID));
        assertThat(result.getReason().get(), is("Invalid risk"));

        // Test with equal scores - should return this
        Risk equal = Risk.of(SMALL, "Equal risk");
        result = low.max(equal);
        assertThat(result.getReason().get(), is("Low risk"));
    }
}
