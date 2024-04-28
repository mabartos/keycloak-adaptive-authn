package org.keycloak.adaptive.context.ip.client;

import inet.ipaddr.IPAddress;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

import static org.keycloak.adaptive.context.ip.IpAddressUtils.FORWARDED_FOR_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.IP_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.getIpAddressFromHeader;

public class HeaderIpAddressContext implements IpAddressContext {
    private final KeycloakSession session;

    private IPAddress data;
    private boolean isInitialized;

    public HeaderIpAddressContext(KeycloakSession session) {
        this.session = session;
        initData();
    }

    @Override
    public void initData() {
        var ipAddress = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRequestHeaders)
                .map(headers -> getIpAddressFromHeader(headers, "Forwarded", FORWARDED_FOR_PATTERN)
                        .or(() -> getIpAddressFromHeader(headers, "X-Forwarded-For", IP_PATTERN)))
                .filter(Optional::isPresent)
                .map(Optional::get);

        if (ipAddress.isPresent()) {
            this.data = ipAddress.get();
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
