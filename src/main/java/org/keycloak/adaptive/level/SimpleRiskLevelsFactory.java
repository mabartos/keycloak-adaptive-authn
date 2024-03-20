package org.keycloak.adaptive.level;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.level.RiskLevelsFactory;
import org.keycloak.adaptive.spi.level.RiskLevelsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SimpleRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "simple-risk-levels";
    public static final RiskLevelsProvider SINGLETON = new SimpleRiskLevelsProvider();

    @Override
    public RiskLevelsProvider create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
