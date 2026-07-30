package io.github.mabartos.evaluator.ip;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAllowlistEvaluatorConfigTest {

    @Test
    void realmAttributeNames_followEvaluatorAdditionalSettingConvention() {
        assertEquals(
                "adaptive-evaluator-ipv4-allowlist-IpAllowlistRiskEvaluator",
                IpAllowlistEvaluatorConfig.ALLOWLIST_IPV4_CONFIG);
        assertEquals(
                "adaptive-evaluator-allowlisted-score-IpAllowlistRiskEvaluator",
                IpAllowlistEvaluatorConfig.ALLOWLISTED_SCORE_CONFIG);
        assertEquals(
                "adaptive-evaluator-not-allowlisted-score-IpAllowlistRiskEvaluator",
                IpAllowlistEvaluatorConfig.NOT_ALLOWLISTED_SCORE_CONFIG);
    }

    @Test
    void splitEntries_trimsAndSkipsBlankParts() {
        assertEquals(List.of("10.0.0.1", "203.0.113.1"),
                IpAllowlistEvaluatorConfig.splitEntries(" 10.0.0.1 , , 203.0.113.1 "));
    }
}
