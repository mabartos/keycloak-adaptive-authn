package io.github.mabartos.evaluator.location;

import io.github.mabartos.evaluator.EvaluatorSettingUtils;
import io.github.mabartos.spi.level.Risk;
import org.keycloak.models.RealmModel;

import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;

/**
 * Realm-configurable settings for {@link KnownLocationRiskEvaluator}.
 */
public final class KnownLocationEvaluatorConfig {
    public static final String FIRST_LOCATION_SCORE_SETTING_KEY = "first-location-score";
    public static final String KNOWN_LOCATION_SCORE_SETTING_KEY = "known-location-score";
    public static final String SAME_COUNTRY_SCORE_SETTING_KEY = "same-country-score";
    public static final String NEW_COUNTRY_SCORE_SETTING_KEY = "new-country-score";

    public static final Risk.Score DEFAULT_FIRST_LOCATION_SCORE = VERY_SMALL;
    public static final Risk.Score DEFAULT_KNOWN_LOCATION_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_SAME_COUNTRY_SCORE = VERY_SMALL;
    public static final Risk.Score DEFAULT_NEW_COUNTRY_SCORE = MEDIUM;

    private KnownLocationEvaluatorConfig() {
    }

    public static Risk.Score firstLocationScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, FIRST_LOCATION_SCORE_SETTING_KEY, DEFAULT_FIRST_LOCATION_SCORE);
    }

    public static Risk.Score knownLocationScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, KNOWN_LOCATION_SCORE_SETTING_KEY, DEFAULT_KNOWN_LOCATION_SCORE);
    }

    public static Risk.Score sameCountryScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, SAME_COUNTRY_SCORE_SETTING_KEY, DEFAULT_SAME_COUNTRY_SCORE);
    }

    public static Risk.Score newCountryScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, NEW_COUNTRY_SCORE_SETTING_KEY, DEFAULT_NEW_COUNTRY_SCORE);
    }
}
