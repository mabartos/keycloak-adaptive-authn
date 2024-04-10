package org.keycloak.adaptive.context.agent;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.representations.account.DeviceRepresentation;

public interface UserAgentContext extends UserContext<DeviceRepresentation> {
}
