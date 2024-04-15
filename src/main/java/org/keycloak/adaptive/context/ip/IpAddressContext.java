package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import org.keycloak.adaptive.spi.factor.UserContext;

import java.util.List;

public interface IpAddressContext extends UserContext<List<IPAddress>> {
}
