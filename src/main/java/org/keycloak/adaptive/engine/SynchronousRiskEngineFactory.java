package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.RiskEngineFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SynchronousRiskEngineFactory implements RiskEngineFactory {
    public static final String PROVIDER_ID = "sync-risk-engine";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new SynchronousRiskEngine(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
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
}
