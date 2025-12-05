package io.github.mabartos.level;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WeightTest {

    @Test
    public void testWeightConstants() {
        assertThat(Weight.NEGLIGIBLE, is(0.2));
        assertThat(Weight.LOW, is(0.5));
        assertThat(Weight.NORMAL, is(0.8));
        assertThat(Weight.IMPORTANT, is(1.0));
        assertThat(Weight.DEFAULT, is(Weight.NORMAL));
    }

    @Test
    public void testWeightOrdering() {
        assertThat(Weight.NEGLIGIBLE < Weight.LOW, is(true));
        assertThat(Weight.LOW < Weight.NORMAL, is(true));
        assertThat(Weight.NORMAL < Weight.IMPORTANT, is(true));
    }

    @Test
    public void testWeightValidRange() {
        assertThat(Weight.NEGLIGIBLE >= 0.0, is(true));
        assertThat(Weight.NEGLIGIBLE <= 1.0, is(true));

        assertThat(Weight.LOW >= 0.0, is(true));
        assertThat(Weight.LOW <= 1.0, is(true));

        assertThat(Weight.NORMAL >= 0.0, is(true));
        assertThat(Weight.NORMAL <= 1.0, is(true));

        assertThat(Weight.IMPORTANT >= 0.0, is(true));
        assertThat(Weight.IMPORTANT <= 1.0, is(true));
    }

    @Test
    public void testDefaultWeight() {
        assertThat(Weight.DEFAULT, is(0.8));
        assertThat(Weight.DEFAULT, is(Weight.NORMAL));
    }

    @Test
    public void testWeightDifferences() {
        double negligibleToLow = Weight.LOW - Weight.NEGLIGIBLE;
        double lowToNormal = Weight.NORMAL - Weight.LOW;
        double normalToImportant = Weight.IMPORTANT - Weight.NORMAL;

        assertThat(negligibleToLow, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(lowToNormal, is(org.hamcrest.Matchers.closeTo(0.3, 0.001)));
        assertThat(normalToImportant, is(org.hamcrest.Matchers.closeTo(0.2, 0.001)));
    }
}
