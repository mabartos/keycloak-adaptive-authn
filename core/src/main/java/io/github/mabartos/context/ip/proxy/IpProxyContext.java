package io.github.mabartos.context.ip.proxy;

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.DeviceContext;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public abstract class IpProxyContext extends DeviceContext<Set<IPAddress>> {

    public IpProxyContext(KeycloakSession session) {
        super(session);
    }
}
