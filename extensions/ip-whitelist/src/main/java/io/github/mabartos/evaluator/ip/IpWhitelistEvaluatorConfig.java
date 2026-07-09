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
 * Realm attribute configuration for {@link IpWhitelistRiskEvaluator}.
 * <p>
 * Attribute names follow {@link RiskEvaluatorFactory#getAdditionalSettingConfig(Class, String)}.
 * Exposed in the risk-based policies tab via {@link IpWhitelistRiskEvaluatorFactory#getAdditionalAdminConfigProperties()}.
 */
public final class IpWhitelistEvaluatorConfig {
    private static final Logger logger = Logger.getLogger(IpWhitelistEvaluatorConfig.class);

    static final String ENTRY_DELIMITER = ",";

    public static final String IPV4_WHITELIST_SETTING_KEY = "ipv4-whitelist";
    public static final String WHITELISTED_SCORE_SETTING_KEY = "whitelisted-score";
    public static final String NOT_WHITELISTED_SCORE_SETTING_KEY = "not-whitelisted-score";

    public static final String WHITELIST_IPV4_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpWhitelistRiskEvaluator.class, IPV4_WHITELIST_SETTING_KEY);
    public static final String WHITELISTED_SCORE_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpWhitelistRiskEvaluator.class, WHITELISTED_SCORE_SETTING_KEY);
    public static final String NOT_WHITELISTED_SCORE_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            IpWhitelistRiskEvaluator.class, NOT_WHITELISTED_SCORE_SETTING_KEY);

    public static final Risk.Score DEFAULT_WHITELISTED_SCORE = NEGATIVE_LOW;
    public static final Risk.Score DEFAULT_NOT_WHITELISTED_SCORE = NONE;

    public static List<String> configurableScoreNames() {
        return Arrays.stream(Risk.Score.values())
                .filter(score -> score != Risk.Score.INVALID)
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableList());
    }

    private IpWhitelistEvaluatorConfig() {
    }

    public static List<String> whitelistEntries(RealmModel realm) {
        return Optional.ofNullable(realm)
                .map(r -> r.getAttribute(WHITELIST_IPV4_CONFIG))
                .filter(StringUtil::isNotBlank)
                .map(IpWhitelistEvaluatorConfig::splitEntries)
                .filter(entries -> !entries.isEmpty())
                .orElseGet(Collections::emptyList);
    }

    public static Risk.Score whitelistedScore(RealmModel realm) {
        return readScore(realm, WHITELISTED_SCORE_CONFIG, DEFAULT_WHITELISTED_SCORE);
    }

    public static Risk.Score notWhitelistedScore(RealmModel realm) {
        return readScore(realm, NOT_WHITELISTED_SCORE_CONFIG, DEFAULT_NOT_WHITELISTED_SCORE);
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
                .map(IpWhitelistEvaluatorConfig::parseScore)
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
