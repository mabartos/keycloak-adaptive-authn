package io.github.mabartos.evaluator.ip;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpWhitelistEvaluatorConfigTest {

    @Test
    void realmAttributeNames_followEvaluatorAdditionalSettingConvention() {
        assertEquals(
                "adaptive-evaluator-ipv4-whitelist-IpWhitelistRiskEvaluator",
                IpWhitelistEvaluatorConfig.WHITELIST_IPV4_CONFIG);
        assertEquals(
                "adaptive-evaluator-whitelisted-score-IpWhitelistRiskEvaluator",
                IpWhitelistEvaluatorConfig.WHITELISTED_SCORE_CONFIG);
        assertEquals(
                "adaptive-evaluator-not-whitelisted-score-IpWhitelistRiskEvaluator",
                IpWhitelistEvaluatorConfig.NOT_WHITELISTED_SCORE_CONFIG);
    }

    @Test
    void splitEntries_trimsAndSkipsBlankParts() {
        assertEquals(List.of("10.0.0.1", "203.0.113.1"),
                IpWhitelistEvaluatorConfig.splitEntries(" 10.0.0.1 , , 203.0.113.1 "));
    }
}
