package org.keycloak.adaptive.context.ip.client;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.Optional;

import static org.keycloak.adaptive.context.ip.IpAddressUtils.FORWARDED_FOR_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.IP_PATTERN;
import static org.keycloak.adaptive.context.ip.IpAddressUtils.getIpAddressFromHeader;

public class HeaderIpAddressContext extends IpAddressContext {
    private final KeycloakSession session;

    public HeaderIpAddressContext(KeycloakSession session) {
        this.session = session;
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
}
