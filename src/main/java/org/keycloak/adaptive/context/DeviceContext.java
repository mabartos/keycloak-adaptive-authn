package org.keycloak.adaptive.context;

import org.keycloak.adaptive.spi.factor.UserContext;
import org.keycloak.representations.account.DeviceRepresentation;

public interface DeviceContext extends UserContext<DeviceRepresentation> {
}
