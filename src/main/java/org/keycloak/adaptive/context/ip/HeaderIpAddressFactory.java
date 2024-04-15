package org.keycloak.adaptive.context.ip;

import org.keycloak.Config;
import org.keycloak.adaptive.spi.factor.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class HeaderIpAddressFactory implements UserContextFactory<IpAddressContext> {
    public static final String PROVIDER_ID = "header-ip-address-context";

    @Override
    public IpAddressContext create(KeycloakSession session) {
        return new HeaderIpAddressProvider(session);
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
