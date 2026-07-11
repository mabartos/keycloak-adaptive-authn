package io.github.mabartos.evaluator.browser;

import io.github.mabartos.evaluator.EvaluatorSettingUtils;
import io.github.mabartos.spi.level.Risk;
import org.keycloak.models.RealmModel;

import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;

/**
 * Realm-configurable settings for {@link BrowserRiskEvaluator}.
 */
public final class BrowserEvaluatorConfig {
    public static final String KNOWN_BROWSER_SCORE_SETTING_KEY = "known-browser-score";
    public static final String UNKNOWN_BROWSER_SCORE_SETTING_KEY = "unknown-browser-score";

    public static final Risk.Score DEFAULT_KNOWN_BROWSER_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_UNKNOWN_BROWSER_SCORE = MEDIUM;

    private BrowserEvaluatorConfig() {
    }

    public static Risk.Score knownBrowserScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, BrowserRiskEvaluator.class, KNOWN_BROWSER_SCORE_SETTING_KEY, DEFAULT_KNOWN_BROWSER_SCORE);
    }

    public static Risk.Score unknownBrowserScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, BrowserRiskEvaluator.class, UNKNOWN_BROWSER_SCORE_SETTING_KEY, DEFAULT_UNKNOWN_BROWSER_SCORE);
    }
}
