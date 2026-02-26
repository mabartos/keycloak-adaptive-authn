package io.github.mabartos.level;

import org.keycloak.utils.StringUtil;

import java.util.Optional;

/**
 * Result risk from aggregated risks - always (0,1)
 */
public class ResultRisk {
    private static final ResultRisk INVALID = new ResultRisk(-100.0, "Invalid");

    private final double score;
    private final String summary;

    private ResultRisk(double score, String summary) {
        this.score = score;
        this.summary = summary;
    }

    public double getScore() {
        return score;
    }

    public Optional<String> getSummary() {
        return StringUtil.isNotBlank(summary) ? Optional.of(summary) : Optional.empty();
    }

    public boolean isValid() {
        return !this.equals(INVALID) || score != INVALID.score;
    }

    public static ResultRisk invalid() {
        return INVALID;
    }

    public static ResultRisk invalid(String reason) {
        return new ResultRisk(INVALID.score, reason);
    }

    public static ResultRisk of(double score, String summary) {
        return new ResultRisk(score, summary);
    }

    public static ResultRisk of(double score) {
        return of(score, "");
    }

}
