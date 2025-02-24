package org.keycloak.adaptive.engine;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.engine.RiskEngine;
import org.keycloak.adaptive.spi.engine.RiskEngineFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class VirtualThreadsRiskEngineFactory implements RiskEngineFactory, EnvironmentDependentProviderFactory {
    public static final String PROVIDER_ID = "virtual-threads-risk-engine";

    @Override
    public RiskEngine create(KeycloakSession session) {
        return new VirtualThreadsRiskEngine(session);
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

    @Override
    public boolean isSupported(Config.Scope config) {
        // Supported only on >= JDK 21
        return Runtime.version().feature() >= 21;
    }
}
