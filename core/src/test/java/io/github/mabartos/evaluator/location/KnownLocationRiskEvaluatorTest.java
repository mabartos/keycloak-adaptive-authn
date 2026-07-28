package io.github.mabartos.evaluator.location;

import io.github.mabartos.context.location.KnownLocationData;
import io.github.mabartos.context.location.LocationData;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import io.github.mabartos.spi.level.Risk;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;

import java.util.Map;
import java.util.Set;

import static io.github.mabartos.evaluator.location.KnownLocationEvaluatorConfig.NEW_COUNTRY_SCORE_SETTING_KEY;
import static io.github.mabartos.spi.level.Risk.Score.HIGH;
import static io.github.mabartos.spi.level.Risk.Score.MEDIUM;
import static io.github.mabartos.spi.level.Risk.Score.NEGATIVE_LOW;
import static io.github.mabartos.spi.level.Risk.Score.VERY_SMALL;
import static io.github.mabartos.test.RealmModelTestSupport.realmWithAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KnownLocationRiskEvaluatorTest {

    private final TestEvaluator evaluator = new TestEvaluator();

    @Test
    void calculateLocationRisk_returnsNewCountryScoreByDefault() {
        var realm = realmWithAttributes(Map.of());
        var current = location("Germany", "Berlin");
        var known = Set.of(location("France", "Paris"));

        var risk = evaluator.calculate(realm, current, known);

        assertEquals(MEDIUM, risk.getScore());
    }

    @Test
    void calculateLocationRisk_usesConfiguredNewCountryScore() {
        var key = RiskEvaluatorFactory.getAdditionalSettingConfig(
                KnownLocationRiskEvaluator.class, NEW_COUNTRY_SCORE_SETTING_KEY);
        var realm = realmWithAttributes(Map.of(key, "HIGH"));
        var current = location("Germany", "Berlin");
        var known = Set.of(location("France", "Paris"));

        var risk = evaluator.calculate(realm, current, known);

        assertEquals(HIGH, risk.getScore());
    }

    @Test
    void calculateLocationRisk_returnsKnownLocationScoreForExactMatch() {
        var realm = realmWithAttributes(Map.of());
        var current = location("France", "Paris");
        var known = Set.of(location("France", "Paris"), location("Germany", "Berlin"));

        var risk = evaluator.calculate(realm, current, known);

        assertEquals(NEGATIVE_LOW, risk.getScore());
    }

    @Test
    void calculateLocationRisk_returnsSameCountryScore() {
        var realm = realmWithAttributes(Map.of());
        var current = location("France", "Lyon");
        var known = Set.of(location("France", "Paris"));

        var risk = evaluator.calculate(realm, current, known);

        assertEquals(VERY_SMALL, risk.getScore());
    }

    private static LocationData location(String country, String city) {
        return KnownLocationData.of(country, city, 1L);
    }

    private static final class TestEvaluator extends KnownLocationRiskEvaluator {
        TestEvaluator() {
            super(null, null);
        }

        Risk calculate(RealmModel realm, LocationData current, Set<LocationData> known) {
            return calculateLocationRisk(realm, current, known);
        }
    }
}
