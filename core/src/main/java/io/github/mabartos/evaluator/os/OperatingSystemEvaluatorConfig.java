package io.github.mabartos.evaluator.os;

import io.github.mabartos.evaluator.EvaluatorSettingUtils;
import io.github.mabartos.spi.level.Risk;
import org.keycloak.models.RealmModel;

import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;

/**
 * Realm-configurable settings for {@link OperatingSystemRiskEvaluator}.
 */
public final class OperatingSystemEvaluatorConfig {
    public static final String KNOWN_OS_SCORE_SETTING_KEY = "known-os-score";
    public static final String LEGACY_WINDOWS_SCORE_SETTING_KEY = "legacy-windows-score";
    public static final String UNKNOWN_OS_SCORE_SETTING_KEY = "unknown-os-score";

    public static final Risk.Score DEFAULT_KNOWN_OS_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_LEGACY_WINDOWS_SCORE = MEDIUM;
    public static final Risk.Score DEFAULT_UNKNOWN_OS_SCORE = MEDIUM;

    private OperatingSystemEvaluatorConfig() {
    }

    public static Risk.Score knownOsScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, OperatingSystemRiskEvaluator.class, KNOWN_OS_SCORE_SETTING_KEY, DEFAULT_KNOWN_OS_SCORE);
    }

    public static Risk.Score legacyWindowsScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, OperatingSystemRiskEvaluator.class, LEGACY_WINDOWS_SCORE_SETTING_KEY, DEFAULT_LEGACY_WINDOWS_SCORE);
    }

    public static Risk.Score unknownOsScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, OperatingSystemRiskEvaluator.class, UNKNOWN_OS_SCORE_SETTING_KEY, DEFAULT_UNKNOWN_OS_SCORE);
    }
}
