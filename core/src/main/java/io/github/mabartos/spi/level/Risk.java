package io.github.mabartos.spi.level;

import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Common risk values
 */
public class Risk {
    public enum Score {
        INVALID,
        NEGATIVE_HIGH,
        NEGATIVE_LOW,
        NONE,
        VERY_SMALL,
        SMALL,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        EXTREME,
    }

    private final Score scoreCategory;
    private final String reason;

    private Risk(Score scoreCategory, String reason) {
        this.scoreCategory = scoreCategory;
        this.reason = reason;
    }

    public Score getScore() {
        return scoreCategory;
    }

    public Optional<String> getReason() {
        return StringUtil.isNotBlank(reason) ? Optional.of(reason) : Optional.empty();
    }

    public boolean isValid() {
        return scoreCategory != Score.INVALID;
    }

    public static Risk of(Score score) {
        return of(score, "");
    }

    public static Risk of(Score score, String reason) {
        return new Risk(score, reason);
    }

    public static Risk invalid(String reason) {
        return new Risk(Score.INVALID, reason);
    }

    public Risk max(Risk risk) {
        if (risk == null || risk.getScore() == Score.INVALID) {
            return this;
        }
        return getScore().ordinal() >= risk.getScore().ordinal() ? this : risk;
    }
}
