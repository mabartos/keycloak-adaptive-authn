package org.keycloak.adaptive.context.ip.client;

import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.spi.context.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

public class DefaultIpAddress extends IpAddressContext {
    private final KeycloakSession session;
    private final IpAddressContext deviceIpContext; // TODO use it more automatically
    private final IpAddressContext headerIpContext;

    public DefaultIpAddress(KeycloakSession session) {
        this.session = session;
        this.deviceIpContext = ContextUtils.getContext(session, DeviceIpAddressContextFactory.PROVIDER_ID);
        this.headerIpContext = ContextUtils.getContext(session, HeaderIpAddressContextFactory.PROVIDER_ID);
    }

    @Override
    public void initData() {
        var ipAddress = Optional.ofNullable(deviceIpContext).map(UserContext::getData)
                .or(() -> Optional.ofNullable(headerIpContext).map(UserContext::getData));

        if (ipAddress.isPresent()) {
            this.data = ipAddress.get();
            this.isInitialized = true;
        } else {
            this.isInitialized = false;
        }
    }
}
