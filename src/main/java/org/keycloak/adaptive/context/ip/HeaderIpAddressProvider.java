package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class HeaderIpAddressProvider implements IpAddressContext {
    private final KeycloakSession session;
    private List<IPAddress> data;
    private boolean isInitialized;

    public HeaderIpAddressProvider(KeycloakSession session) {
        this.session = session;
        initData();
    }

    @Override
    public boolean isDataInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        this.data = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRequestHeaders)
                .map(f -> {
                    var forwarded = Optional.ofNullable(f.getRequestHeader("Forwarded")).orElse(Collections.emptyList());
                    var xForwarded = Optional.ofNullable(f.getRequestHeader("X-Forwarded-For")).orElse(Collections.emptyList());
                    List<String> result = new ArrayList<>(forwarded);
                    result.addAll(xForwarded);
                    return result;
                })
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(f -> Stream.of(f.split(",")))
                .map(IpAddressUtils::getIpAddress)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        this.isInitialized = true;
    }

    @Override
    public List<IPAddress> getData() {
        return data;
    }
}
