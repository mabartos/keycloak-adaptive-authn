package org.keycloak.adaptive.context.location;

import org.keycloak.adaptive.spi.context.RiskEvaluator;
import org.keycloak.adaptive.spi.context.RiskEvaluatorFactory;
import org.keycloak.models.KeycloakSession;

public class LocationRiskEvaluatorFactory implements RiskEvaluatorFactory {
    public static final String PROVIDER_ID = "location-risk-evaluator";

    public static final String NAME = "Location IP API";
    public static final long CACHE_LIFESPAN_SECONDS = 90;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskEvaluator create(KeycloakSession session) {
        return new LocationRiskEvaluator(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
