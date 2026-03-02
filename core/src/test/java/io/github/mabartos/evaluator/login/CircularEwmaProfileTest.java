package io.github.mabartos.evaluator.login;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CircularEwmaProfileTest {

    @Test
    public void testProfileInitialization() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);
        assertThat(profile.isInitialized(), is(false));
        assertThat(profile.getConcentration(), closeTo(0.0, 0.001));
    }

    @Test
    public void testInvalidAlpha() {
        assertThrows(IllegalArgumentException.class, () -> new CircularEwmaProfile(0.0));
        assertThrows(IllegalArgumentException.class, () -> new CircularEwmaProfile(1.5));
        assertThrows(IllegalArgumentException.class, () -> new CircularEwmaProfile(-0.1));
    }

    @Test
    public void testInvalidHour() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);
        assertThrows(IllegalArgumentException.class, () -> profile.update(-1));
        assertThrows(IllegalArgumentException.class, () -> profile.update(24));
        assertThrows(IllegalArgumentException.class, () -> profile.getDeviation(-1));
        assertThrows(IllegalArgumentException.class, () -> profile.getDeviation(24));
    }

    @Test
    public void testConsistentLoginTimes() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);

        // Simulate user always logging in at 8am
        for (int i = 0; i < 20; i++) {
            profile.update(8);
        }

        // Mean should be close to 8
        assertThat(profile.getMeanHour(), closeTo(8.0, 0.1));

        // Concentration should be high (close to 1) for consistent pattern
        assertThat(profile.getConcentration(), greaterThan(0.9));

        // Deviation from mean should be very small
        assertThat(profile.getDeviation(8), closeTo(0.0, 0.1));

        // Login at 3pm (7 hours later) should have large deviation
        assertThat(profile.getDeviation(15), closeTo(7.0, 0.5));
    }

    @Test
    public void testVariableLoginTimes() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);

        // Simulate user logging in at different times
        int[] loginTimes = {8, 14, 20, 10, 16, 22, 9, 15, 21, 11};
        for (int hour : loginTimes) {
            profile.update(hour);
        }

        // Concentration should be lower for dispersed pattern
        assertThat(profile.getConcentration(), lessThan(0.7));
    }

    @Test
    public void testMidnightWraparound() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);

        // User logs in late at night and early morning
        for (int i = 0; i < 10; i++) {
            profile.update(23); // 11pm
            profile.update(0);  // midnight
            profile.update(1);  // 1am
        }

        // Mean should be around midnight (23, 0, or 1)
        double mean = profile.getMeanHour();
        assertThat(mean < 2.0 || mean > 22.0, is(true));

        // 11pm should be close to midnight in circular distance
        double deviation23 = profile.getDeviation(23);
        assertThat(deviation23, lessThan(2.0));

        // 1am should be close to midnight in circular distance
        double deviation1 = profile.getDeviation(1);
        assertThat(deviation1, lessThan(2.0));

        // Noon should be far from midnight (about 12 hours)
        double deviation12 = profile.getDeviation(12);
        assertThat(deviation12, greaterThan(10.0));
    }

    @Test
    public void testDeviationCalculation() {
        CircularEwmaProfile profile = new CircularEwmaProfile(0.2);

        // Build a pattern around 9am
        for (int i = 0; i < 15; i++) {
            profile.update(9);
        }

        // Test various deviations
        assertThat(profile.getDeviation(9), closeTo(0.0, 0.1));   // Same time
        assertThat(profile.getDeviation(11), closeTo(2.0, 0.5));  // 2 hours later
        assertThat(profile.getDeviation(7), closeTo(2.0, 0.5));   // 2 hours earlier
        assertThat(profile.getDeviation(15), closeTo(6.0, 0.5));  // 6 hours later
        assertThat(profile.getDeviation(21), closeTo(12.0, 1.0)); // Opposite time
    }

    @Test
    public void testProfileStatePreservation() {
        CircularEwmaProfile profile1 = new CircularEwmaProfile(0.15);

        // Train the profile
        for (int i = 0; i < 10; i++) {
            profile1.update(8);
        }

        double meanSin = profile1.getMeanSin();
        double meanCos = profile1.getMeanCos();
        double alpha = profile1.getAlpha();

        // Create new profile from saved state
        CircularEwmaProfile profile2 = new CircularEwmaProfile(alpha, meanSin, meanCos);

        // Should have identical behavior
        assertThat(profile2.getMeanHour(), closeTo(profile1.getMeanHour(), 0.001));
        assertThat(profile2.getConcentration(), closeTo(profile1.getConcentration(), 0.001));
        assertThat(profile2.getDeviation(8), closeTo(profile1.getDeviation(8), 0.001));
        assertThat(profile2.getDeviation(15), closeTo(profile1.getDeviation(15), 0.001));
    }

    @Test
    public void testAlphaEffect() {
        // High alpha (0.8) - quick adaptation to new data
        CircularEwmaProfile quickProfile = new CircularEwmaProfile(0.8);

        // Low alpha (0.1) - slow adaptation, more weight to history
        CircularEwmaProfile slowProfile = new CircularEwmaProfile(0.1);

        // Both see 8am logins initially
        for (int i = 0; i < 10; i++) {
            quickProfile.update(8);
            slowProfile.update(8);
        }

        // Then both see a few 14:00 logins
        for (int i = 0; i < 3; i++) {
            quickProfile.update(14);
            slowProfile.update(14);
        }

        // Quick profile should have adapted more towards 14:00
        double quickMean = quickProfile.getMeanHour();
        double slowMean = slowProfile.getMeanHour();

        // Quick profile mean should be closer to 14 than slow profile
        double quickDistTo14 = Math.min(Math.abs(14 - quickMean), 24 - Math.abs(14 - quickMean));
        double slowDistTo14 = Math.min(Math.abs(14 - slowMean), 24 - Math.abs(14 - slowMean));

        assertThat(quickDistTo14, lessThan(slowDistTo14));
    }

    @Test
    public void testRealWorldScenario() {
        // Simulate a real-world scenario: office worker logging in weekdays at ~9am
        CircularEwmaProfile profile = new CircularEwmaProfile(0.15);

        // First 20 logins around 9am (with some variation)
        int[] normalLogins = {9, 8, 9, 9, 10, 8, 9, 9, 9, 8, 9, 10, 9, 8, 9, 9, 9, 10, 8, 9};
        for (int hour : normalLogins) {
            profile.update(hour);
        }

        // Pattern should be established around 9am
        assertThat(profile.getMeanHour(), closeTo(9.0, 0.5));
        assertThat(profile.getConcentration(), greaterThan(0.8));

        // Normal login time should have low deviation
        assertThat(profile.getDeviation(9), closeTo(0.0, 0.5));

        // Slightly unusual time (lunch) should have moderate deviation
        assertThat(profile.getDeviation(12), closeTo(3.0, 0.5));

        // Very unusual time (3am - potential fraud) should have high deviation
        double fraudDeviation = profile.getDeviation(3);
        assertThat(fraudDeviation, greaterThan(5.0));

        // Weekend night login (suspicious)
        double midnightDeviation = profile.getDeviation(0);
        assertThat(midnightDeviation, greaterThan(8.0));
    }
}
