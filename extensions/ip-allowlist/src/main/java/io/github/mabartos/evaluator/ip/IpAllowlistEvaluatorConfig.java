package io.github.mabartos.evaluator.ip;

import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.NONE;

/**
 * Realm attribute configuration for {@link IpAllowlistRiskEvaluator}.
 * <p>
 * Attribute names follow {@link RiskEvaluatorFactory#getAdditionalSettingConfig(Class, String)}.
 * Exposed in the risk-based policies tab via {@link IpAllowlistRiskEvaluatorFactory#getAdditionalAdminConfigProperties()}.
 */
public final class IpAllowlistEvaluatorConfig {
    private static final Logger logger = Logger.getLogger(IpAllowlistEvaluatorConfig.class);

    static final String ENTRY_DELIMITER = ",";

    public static final String IPV4_ALLOWLIST_SETTING_KEY = "ipv4-allowlist";
    public static final String ALLOWLISTED_SCORE_SETTING_KEY = "allowlisted-score";
    public static final String NOT_ALLOWLISTED_SCORE_SETTING_KEY = "not-allowlisted-score";

    public static final String ALLOWLIST_IPV4_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpAllowlistRiskEvaluator.class, IPV4_ALLOWLIST_SETTING_KEY);
    public static final String ALLOWLISTED_SCORE_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpAllowlistRiskEvaluator.class, ALLOWLISTED_SCORE_SETTING_KEY);
    public static final String NOT_ALLOWLISTED_SCORE_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpAllowlistRiskEvaluator.class, NOT_ALLOWLISTED_SCORE_SETTING_KEY);

    public static final Risk.Score DEFAULT_ALLOWLISTED_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_NOT_ALLOWLISTED_SCORE = NONE;

    public static List<String> configurableScoreNames() {
        return Arrays.stream(Risk.Score.values())
                .filter(score -> score != Risk.Score.INVALID)
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableList());
    }

    private IpAllowlistEvaluatorConfig() {
    }

    public static List<String> allowlistEntries(RealmModel realm) {
        return Optional.ofNullable(realm)
                .map(r -> r.getAttribute(ALLOWLIST_IPV4_CONFIG))
                .filter(StringUtil::isNotBlank)
                .map(IpAllowlistEvaluatorConfig::splitEntries)
                .filter(entries -> !entries.isEmpty())
                .orElseGet(Collections::emptyList);
    }

    public static Risk.Score allowlistedScore(RealmModel realm) {
        return readScore(realm, ALLOWLISTED_SCORE_CONFIG, DEFAULT_ALLOWLISTED_SCORE);
    }

    public static Risk.Score notAllowlistedScore(RealmModel realm) {
        return readScore(realm, NOT_ALLOWLISTED_SCORE_CONFIG, DEFAULT_NOT_ALLOWLISTED_SCORE);
    }

    static List<String> splitEntries(String value) {
        return Arrays.stream(value.split(ENTRY_DELIMITER))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }

    private static Risk.Score readScore(RealmModel realm, String attribute, Risk.Score defaultValue) {
        return Optional.ofNullable(realm)
                .map(r -> r.getAttribute(attribute))
                .filter(StringUtil::isNotBlank)
                .map(IpAllowlistEvaluatorConfig::parseScore)
                .filter(score -> score != null && score != Risk.Score.INVALID)
                .orElse(defaultValue);
    }

    private static Risk.Score parseScore(String value) {
        try {
            var score = Risk.Score.valueOf(value.trim().toUpperCase());
            return score == Risk.Score.INVALID ? null : score;
        } catch (IllegalArgumentException e) {
            logger.tracef("Invalid risk score realm attribute '%s', using default", value);
            return null;
        }
    }
}
