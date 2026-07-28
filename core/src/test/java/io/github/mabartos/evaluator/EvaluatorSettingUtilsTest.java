package io.github.mabartos.evaluator;

import io.github.mabartos.evaluator.location.KnownLocationRiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.mabartos.test.RealmModelTestSupport.realmWithAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluatorSettingUtilsTest {

    private static final String SETTING_KEY = "new-country-score";
    private static final String ATTRIBUTE_KEY = RiskEvaluatorFactory.getAdditionalSettingConfig(
            KnownLocationRiskEvaluator.class, SETTING_KEY);

    @Test
    void getScore_returnsDefaultWhenAttributeMissing() {
        var realm = realmWithAttributes(Map.of());

        var score = EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, SETTING_KEY, Risk.Score.MEDIUM);

        assertEquals(Risk.Score.MEDIUM, score);
    }

    @Test
    void getScore_returnsConfiguredValue() {
        var realm = realmWithAttributes(Map.of(ATTRIBUTE_KEY, "HIGH"));

        var score = EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, SETTING_KEY, Risk.Score.MEDIUM);

        assertEquals(Risk.Score.HIGH, score);
    }

    @Test
    void getScore_returnsDefaultForInvalidValue() {
        var realm = realmWithAttributes(Map.of(ATTRIBUTE_KEY, "NOT_A_SCORE"));

        var score = EvaluatorSettingUtils.getScore(
                realm, KnownLocationRiskEvaluator.class, SETTING_KEY, Risk.Score.MEDIUM);

        assertEquals(Risk.Score.MEDIUM, score);
    }

    @Test
    void getInt_returnsDefaultWhenAttributeMissing() {
        var realm = realmWithAttributes(Map.of());

        assertEquals(10, EvaluatorSettingUtils.getInt(realm, KnownLocationRiskEvaluator.class, "ttl-days", 10));
    }

    @Test
    void getInt_returnsConfiguredValue() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "ttl-days");
        var realm = realmWithAttributes(Map.of(key, "45"));

        assertEquals(45, EvaluatorSettingUtils.getInt(realm, KnownLocationRiskEvaluator.class, "ttl-days", 90));
    }

    @Test
    void getInt_returnsDefaultForNegativeValue() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "ttl-days");
        var realm = realmWithAttributes(Map.of(key, "-1"));

        assertEquals(90, EvaluatorSettingUtils.getInt(realm, KnownLocationRiskEvaluator.class, "ttl-days", 90));
    }

    @Test
    void getInt_returnsDefaultForNonNumericValue() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "ttl-days");
        var realm = realmWithAttributes(Map.of(key, "abc"));

        assertEquals(90, EvaluatorSettingUtils.getInt(realm, KnownLocationRiskEvaluator.class, "ttl-days", 90));
    }

    @Test
    void getPositiveInt_enforcesMinimum() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "max-stored-locations");
        var realm = realmWithAttributes(Map.of(key, "0"));

        assertEquals(10, EvaluatorSettingUtils.getPositiveInt(
                realm, KnownLocationRiskEvaluator.class, "max-stored-locations", 1, 10));
    }

    @Test
    void getPositiveInt_returnsValueAtMinimum() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "max-stored-locations");
        var realm = realmWithAttributes(Map.of(key, "1"));

        assertEquals(1, EvaluatorSettingUtils.getPositiveInt(
                realm, KnownLocationRiskEvaluator.class, "max-stored-locations", 1, 10));
    }

    @Test
    void attributeKey_followsConvention() {
        assertEquals(
                "adaptive-evaluator-ttl-days-KnownLocationRiskEvaluator",
                RiskEvaluatorFactory.getAdditionalSettingConfig(KnownLocationRiskEvaluator.class, "ttl-days"));
    }

    @Test
    void configurableScoreNames_excludesInvalid() {
        assertFalse(EvaluatorSettingUtils.configurableScoreNames().contains(Risk.Score.INVALID.name()));
        assertTrue(EvaluatorSettingUtils.configurableScoreNames().contains(Risk.Score.HIGH.name()));
    }

    @Test
    void isValidScoreName_acceptsKnownScores() {
        assertTrue(EvaluatorSettingUtils.isValidScoreName("HIGH"));
        assertFalse(EvaluatorSettingUtils.isValidScoreName("INVALID"));
        assertFalse(EvaluatorSettingUtils.isValidScoreName("unknown"));
    }
}
