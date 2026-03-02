package io.github.mabartos.evaluator.login;

/**
 * Circular Exponentially Weighted Moving Average (EWMA) profile for tracking time patterns.
 * <p>
 * This class uses circular statistics to handle the wraparound nature of time (24 hours).
 * Traditional statistics fail for time because 23:00 is actually close to 01:00 (2 hours),
 * not 22 hours away. This implementation converts hours to angles on a circle and uses
 * sine/cosine components to calculate meaningful averages and deviations.
 * <p>
 * <strong>Example:</strong> If a user logs in at 23:00, 00:00, and 01:00, the mean will be
 * around midnight (not 8:00 as simple averaging would give).
 *
 * @see TimePatternRiskEvaluator
 */
public class CircularEwmaProfile {

    private double meanSin = 0;
    private double meanCos = 0;
    private final double alpha;

    /**
     * Creates a new circular EWMA profile.
     *
     * @param alpha Smoothing factor (0 < alpha <= 1). Lower values give more weight to historical data.
     *              Recommended: 0.1-0.3 for login patterns.
     */
    public CircularEwmaProfile(double alpha) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("Alpha must be between 0 and 1");
        }
        this.alpha = alpha;
    }

    /**
     * Creates a profile from stored state.
     */
    public CircularEwmaProfile(double alpha, double meanSin, double meanCos) {
        this(alpha);
        this.meanSin = meanSin;
        this.meanCos = meanCos;
    }

    /**
     * Updates the profile with a new time observation.
     *
     * @param hour Hour of the day (0-23)
     */
    public void update(int hour) {
        if (hour < 0 || hour >= 24) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }

        double angle = toAngle(hour);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        meanSin = alpha * sin + (1 - alpha) * meanSin;
        meanCos = alpha * cos + (1 - alpha) * meanCos;
    }

    /**
     * Gets the mean hour from the profile.
     *
     * @return Mean hour (0-24)
     */
    public double getMeanHour() {
        double meanAngle = Math.atan2(meanSin, meanCos);
        if (meanAngle < 0) {
            meanAngle += 2 * Math.PI;
        }
        return 24.0 * meanAngle / (2 * Math.PI);
    }

    /**
     * Calculates the deviation in hours from the mean pattern.
     * This handles the circular nature of time (e.g., 23:00 is close to 01:00).
     *
     * @param hour Hour to compare (0-23)
     * @return Deviation in hours (0-12)
     */
    public double getDeviation(int hour) {
        if (hour < 0 || hour >= 24) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }

        double currentAngle = toAngle(hour);
        double meanAngle = Math.atan2(meanSin, meanCos);

        double diff = Math.abs(currentAngle - meanAngle);
        if (diff > Math.PI) {
            diff = 2 * Math.PI - diff;
        }

        return diff * 24.0 / (2 * Math.PI);
    }

    /**
     * Checks if the profile has been initialized with data.
     */
    public boolean isInitialized() {
        return meanSin != 0 || meanCos != 0;
    }

    /**
     * Gets the concentration parameter (resultant vector length).
     * Returns a value between 0 and 1, where:
     * - 0 means completely dispersed (logins at all times)
     * - 1 means perfectly concentrated (always the same time)
     */
    public double getConcentration() {
        return Math.sqrt(meanSin * meanSin + meanCos * meanCos);
    }

    private double toAngle(int hour) {
        return 2 * Math.PI * hour / 24.0;
    }

    public double getMeanSin() {
        return meanSin;
    }

    public double getMeanCos() {
        return meanCos;
    }

    public double getAlpha() {
        return alpha;
    }
}
