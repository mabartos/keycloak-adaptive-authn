package org.keycloak.adaptive.context.ip.client;

import inet.ipaddr.IPAddress;
import org.keycloak.adaptive.context.ContextUtils;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.adaptive.context.DeviceContextFactory;
import org.keycloak.adaptive.context.ip.IpAddressUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.account.DeviceRepresentation;

import java.util.Optional;

public class DeviceIpAddressContext implements IpAddressContext {
    private final KeycloakSession session;
    private final DeviceContext deviceContext;

    private IPAddress data;
    private boolean isInitialized;

    public DeviceIpAddressContext(KeycloakSession session) {
        this.session = session;
        this.deviceContext = ContextUtils.getContext(session, DeviceContextFactory.PROVIDER_ID);
        initData();
    }

    @Override
    public void initData() {
        var ip = Optional.ofNullable(deviceContext.getData())
                .map(DeviceRepresentation::getIpAddress)
                .flatMap(IpAddressUtils::getIpAddress);

        if (ip.isPresent()) {
            this.data = ip.get();
            this.isInitialized = true;
        } else {
            this.isInitialized = false;
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public IPAddress getData() {
        return data;
    }
}
