package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import org.keycloak.adaptive.spi.context.UserContext;

import java.util.Set;

public interface IpProxyContext extends UserContext<Set<IPAddress>> {
}
