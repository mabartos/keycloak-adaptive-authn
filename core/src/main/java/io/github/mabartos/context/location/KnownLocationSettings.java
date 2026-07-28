package io.github.mabartos.context.location;

import io.github.mabartos.evaluator.EvaluatorSettingUtils;
import io.github.mabartos.evaluator.location.KnownLocationRiskEvaluator;
import io.github.mabartos.spi.evaluator.RiskEvaluatorFactory;
import org.keycloak.models.RealmModel;

/**
 * Realm-configurable persistence settings for {@link KnownLocationContext}.
 */
public final class KnownLocationSettings {
    public static final String TTL_DAYS_SETTING_KEY = "ttl-days";
    public static final String MAX_STORED_LOCATIONS_SETTING_KEY = "max-stored-locations";

    public static final String TTL_DAYS_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            KnownLocationRiskEvaluator.class, TTL_DAYS_SETTING_KEY);
    public static final String MAX_STORED_LOCATIONS_CONFIG = RiskEvaluatorFactory.getAdditionalSettingConfig(
            KnownLocationRiskEvaluator.class, MAX_STORED_LOCATIONS_SETTING_KEY);

    public static final int DEFAULT_TTL_DAYS = 90;
    public static final int DEFAULT_MAX_STORED_LOCATIONS = 10;

    private KnownLocationSettings() {
    }

    public static int ttlDays(RealmModel realm) {
        return EvaluatorSettingUtils.getInt(
                realm, KnownLocationRiskEvaluator.class, TTL_DAYS_SETTING_KEY, DEFAULT_TTL_DAYS);
    }

    public static int maxStoredLocations(RealmModel realm) {
        return EvaluatorSettingUtils.getPositiveInt(
                realm,
                KnownLocationRiskEvaluator.class,
                MAX_STORED_LOCATIONS_SETTING_KEY,
                1,
                DEFAULT_MAX_STORED_LOCATIONS);
    }
}
