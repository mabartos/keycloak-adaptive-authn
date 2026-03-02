package io.github.mabartos.context.ip.client;

import inet.ipaddr.IPAddress;
import io.github.mabartos.context.DeviceContext;
import org.keycloak.models.KeycloakSession;

public abstract class IpAddressContext extends DeviceContext<IPAddress> {

    public IpAddressContext(KeycloakSession session) {
        super(session);
    }
}
