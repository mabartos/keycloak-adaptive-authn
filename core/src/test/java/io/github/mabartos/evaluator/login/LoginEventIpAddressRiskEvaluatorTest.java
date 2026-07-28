package io.github.mabartos.evaluator.login;

import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;

import java.util.Map;

import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.INVALID;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.SMALL;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;
import static io.github.mabartos.test.RealmModelTestSupport.realmWithAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginEventIpAddressRiskEvaluatorTest {

    private final TestEvaluator evaluator = new TestEvaluator();

    @Test
    void evaluateIpHistory_returnsNeverSeenScore() {
        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of()), 0, 10);

        assertEquals(HIGH, risk.getScore());
    }

    @Test
    void evaluateIpHistory_returnsFrequentIpScore() {
        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of()), 4, 12);

        assertEquals(NEGATIVE_LOW, risk.getScore());
    }

    @Test
    void evaluateIpHistory_returnsOccasionalIpScore() {
        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of()), 2, 12);

        assertEquals(VERY_SMALL, risk.getScore());
    }

    @Test
    void evaluateIpHistory_returnsInvalidWhenNotEnoughHistory() {
        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of()), 1, 3);

        assertEquals(INVALID, risk.getScore());
    }

    @Test
    void evaluateIpHistory_usesConfiguredNeverSeenScore() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(
                LoginEventIpAddressRiskEvaluator.class,
                LoginEventIpAddressEvaluatorConfig.NEVER_SEEN_IP_SCORE_SETTING_KEY);

        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of(key, "SMALL")), 0, 10);

        assertEquals(SMALL, risk.getScore());
    }

    @Test
    void evaluateIpHistory_usesConfiguredMinLoginEvents() {
        var minEventsKey = RiskEvaluatorFactory.getAdditionalSettingConfig(
                LoginEventIpAddressRiskEvaluator.class,
                LoginEventIpAddressEvaluatorConfig.MIN_LOGIN_EVENTS_SETTING_KEY);

        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of(minEventsKey, "2")), 1, 6);

        assertEquals(VERY_SMALL, risk.getScore());
    }

    @Test
    void evaluateIpHistory_usesConfiguredFrequentIpThresholdDivisor() {
        var divisorKey = RiskEvaluatorFactory.getAdditionalSettingConfig(
                LoginEventIpAddressRiskEvaluator.class,
                LoginEventIpAddressEvaluatorConfig.FREQUENT_IP_THRESHOLD_DIVISOR_SETTING_KEY);

        var risk = evaluator.evaluateHistory(realmWithAttributes(Map.of(divisorKey, "4")), 3, 12);

        assertEquals(NEGATIVE_LOW, risk.getScore());
    }

    private static final class TestEvaluator extends LoginEventIpAddressRiskEvaluator {
        TestEvaluator() {
            super(null, null);
        }

        Risk evaluateHistory(RealmModel realm, long numberOccurrences, int eventsSize) {
            return evaluateIpHistory(realm, numberOccurrences, eventsSize);
        }
    }
}
