package io.github.mabartos.context.ssf;

import io.github.mabartos.spi.context.UserContextFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class SsfSignalContextFactory implements UserContextFactory<SsfSignalContext>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "ssf-signal-context";

    @Override
    public SsfSignalContext create(KeycloakSession session) {
        return new SsfSignalContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Class<SsfSignalContext> getUserContextClass() {
        return SsfSignalContext.class;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return SsfFeatureSupport.isSsfAvailable();
    }
}
