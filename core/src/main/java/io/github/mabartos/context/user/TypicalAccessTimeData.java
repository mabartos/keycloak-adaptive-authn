package io.github.mabartos.context.user;

import io.github.mabartos.evaluator.login.CircularEwmaProfile;

/**
 * Data object containing typical access time information for a user.
 * This is calculated using circular EWMA statistics to handle the wraparound nature of time.
 */
public class TypicalAccessTimeData {
    private final CircularEwmaProfile profile;
    private final int loginCount;

    public TypicalAccessTimeData(CircularEwmaProfile profile, int loginCount) {
        this.profile = profile;
        this.loginCount = loginCount;
    }

    /**
     * Gets the circular EWMA profile containing the time pattern.
     */
    public CircularEwmaProfile getProfile() {
        return profile;
    }

    /**
     * Gets the typical login hour (0-23) rounded to nearest hour.
     */
    public int getTypicalLoginHour() {
        return (int) Math.round(profile.getMeanHour());
    }

    /**
     * Gets the exact mean login hour (0-24) with decimal precision.
     */
    public double getExactMeanHour() {
        return profile.getMeanHour();
    }

    /**
     * Gets the concentration (0-1) indicating how consistent the login pattern is.
     * 0 = completely random times, 1 = always the same time.
     */
    public double getConcentration() {
        return profile.getConcentration();
    }

    /**
     * Gets the number of logins used to build this profile.
     */
    public int getLoginCount() {
        return loginCount;
    }

    /**
     * Checks if there's sufficient data to make meaningful predictions.
     */
    public boolean hasSufficientData() {
        return loginCount >= TypicalAccessTimeContext.MIN_LOGINS;
    }
}
