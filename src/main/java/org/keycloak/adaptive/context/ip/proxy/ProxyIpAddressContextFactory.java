package org.keycloak.adaptive.context.ip.proxy;

import org.keycloak.adaptive.spi.context.UserContextFactory;
import org.keycloak.models.KeycloakSession;

public class ProxyIpAddressContextFactory implements UserContextFactory<IpProxyContext> {
    public static final String PROVIDER_ID = "default-ip-proxy-context";

    @Override
    public IpProxyContext create(KeycloakSession session) {
        return new ProxyIpAddressContext(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
