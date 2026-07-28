package io.github.mabartos.evaluator;

import io.github.mabartos.spi.evaluator.RiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared runtime reads for evaluator settings stored as realm attributes.
 * Attribute keys follow {@link RiskEvaluatorFactory#getAdditionalSettingConfig(Class, String)}.
 */
public final class EvaluatorSettingUtils {
    private static final Logger logger = Logger.getLogger(EvaluatorSettingUtils.class);

    private EvaluatorSettingUtils() {
    }

    public static List<String> configurableScoreNames() {
        return Arrays.stream(Risk.Score.values())
                .filter(score -> score != Risk.Score.INVALID)
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static boolean isValidScoreName(String value) {
        if (StringUtil.isBlank(value)) {
            return false;
        }
        try {
            var score = Risk.Score.valueOf(value.trim().toUpperCase());
            return score != Risk.Score.INVALID;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Risk.Score getScore(
            RealmModel realm,
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            Risk.Score defaultValue) {
        return Optional.ofNullable(realm)
                .map(r -> r.getAttribute(RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey)))
                .filter(StringUtil::isNotBlank)
                .map(value -> {
                    var score = parseScore(value);
                    return score != null ? score : defaultValue;
                })
                .orElse(defaultValue);
    }

    public static int getInt(
            RealmModel realm,
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            int defaultValue) {
        return getPositiveInt(realm, evaluatorClass, settingKey, 0, defaultValue);
    }

    public static int getPositiveInt(
            RealmModel realm,
            Class<? extends RiskEvaluator> evaluatorClass,
            String settingKey,
            int minimum,
            int defaultValue) {
        var configKey = RiskEvaluatorFactory.getAdditionalSettingConfig(evaluatorClass, settingKey);
        return Optional.ofNullable(realm)
                .map(r -> r.getAttribute(configKey))
                .filter(StringUtil::isNotBlank)
                .flatMap(value -> parseIntAtLeast(value, minimum, configKey, defaultValue))
                .orElse(defaultValue);
    }

    private static Risk.Score parseScore(String value) {
        try {
            var score = Risk.Score.valueOf(value.trim().toUpperCase());
            return score == Risk.Score.INVALID ? null : score;
        } catch (IllegalArgumentException e) {
            logger.tracef("Invalid risk score config value '%s', using default", value);
            return null;
        }
    }

    private static Optional<Integer> parseIntAtLeast(String value, int minimum, String configKey, int defaultValue) {
        try {
            var parsed = Integer.parseInt(value.trim());
            if (parsed < minimum) {
                logger.warnf(
                        "Invalid realm attribute '%s' value '%s' below minimum %d, using default %d",
                        configKey, value, minimum, defaultValue);
                return Optional.empty();
            }
            return Optional.of(parsed);
        } catch (NumberFormatException e) {
            logger.warnf("Invalid realm attribute '%s' value '%s', using default %d", configKey, value, defaultValue);
            return Optional.empty();
        }
    }
}
