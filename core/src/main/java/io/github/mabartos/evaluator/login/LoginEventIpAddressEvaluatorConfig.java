package io.github.mabartos.evaluator.login;

import io.github.mabartos.evaluator.EvaluatorSettingUtils;
import io.github.mabartos.spi.level.Risk;
import org.keycloak.models.RealmModel;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;

/**
 * Realm-configurable settings for {@link LoginEventIpAddressRiskEvaluator}.
 */
public final class LoginEventIpAddressEvaluatorConfig {
    public static final String NEVER_SEEN_IP_SCORE_SETTING_KEY = "never-seen-ip-score";
    public static final String FREQUENT_IP_SCORE_SETTING_KEY = "frequent-ip-score";
    public static final String OCCASIONAL_IP_SCORE_SETTING_KEY = "occasional-ip-score";
    public static final String MIN_LOGIN_EVENTS_SETTING_KEY = "min-login-events";
    public static final String FREQUENT_IP_THRESHOLD_DIVISOR_SETTING_KEY = "frequent-ip-threshold-divisor";

    public static final Risk.Score DEFAULT_NEVER_SEEN_IP_SCORE = HIGH;
    public static final Risk.Score DEFAULT_FREQUENT_IP_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_OCCASIONAL_IP_SCORE = VERY_SMALL;
    public static final int DEFAULT_MIN_LOGIN_EVENTS = 4;
    public static final int DEFAULT_FREQUENT_IP_THRESHOLD_DIVISOR = 3;

    private LoginEventIpAddressEvaluatorConfig() {
    }

    public static Risk.Score neverSeenIpScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, LoginEventIpAddressRiskEvaluator.class, NEVER_SEEN_IP_SCORE_SETTING_KEY, DEFAULT_NEVER_SEEN_IP_SCORE);
    }

    public static Risk.Score frequentIpScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, LoginEventIpAddressRiskEvaluator.class, FREQUENT_IP_SCORE_SETTING_KEY, DEFAULT_FREQUENT_IP_SCORE);
    }

    public static Risk.Score occasionalIpScore(RealmModel realm) {
        return EvaluatorSettingUtils.getScore(
                realm, LoginEventIpAddressRiskEvaluator.class, OCCASIONAL_IP_SCORE_SETTING_KEY, DEFAULT_OCCASIONAL_IP_SCORE);
    }

    public static int minLoginEvents(RealmModel realm) {
        return EvaluatorSettingUtils.getPositiveInt(
                realm,
                LoginEventIpAddressRiskEvaluator.class,
                MIN_LOGIN_EVENTS_SETTING_KEY,
                1,
                DEFAULT_MIN_LOGIN_EVENTS);
    }

    public static int frequentIpThresholdDivisor(RealmModel realm) {
        return EvaluatorSettingUtils.getPositiveInt(
                realm,
                LoginEventIpAddressRiskEvaluator.class,
                FREQUENT_IP_THRESHOLD_DIVISOR_SETTING_KEY,
                1,
                DEFAULT_FREQUENT_IP_THRESHOLD_DIVISOR);
    }
}
