package org.keycloak.adaptive.context.ip.proxy;

import inet.ipaddr.IPAddress;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.adaptive.context.ip.IpAddressUtils;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.adaptive.context.ip.IpAddressUtils.IP_PATTERN;

public class ProxyIpAddressContext implements IpProxyContext {
    private final KeycloakSession session;
    private Set<IPAddress> data;
    private boolean isInitialized;

    public ProxyIpAddressContext(KeycloakSession session) {
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
                .map(headers -> Stream.concat(
                        getIpAddressFromHeader(headers, "Forwarded"),
                        getIpAddressFromHeader(headers, "X-Forwarded-For"))
                )
                .map(f -> f.collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);

        this.isInitialized = true;
    }

    private static Stream<IPAddress> getIpAddressFromHeader(HttpHeaders headers, String headerName) {
        return Optional.ofNullable(headers.getRequestHeader(headerName))
                .flatMap(h -> h.stream().findFirst())
                .map(h -> List.of(h.split(",")))
                .stream()
                .flatMap(Collection::stream)
                .map(f -> {
                    var ipAddress = new HashSet<String>();
                    var matcher = IP_PATTERN.matcher(f);
                    while (matcher.find()) {
                        ipAddress.add(matcher.group());
                    }
                    return ipAddress;
                })
                .flatMap(Collection::stream)
                .filter(StringUtil::isNotBlank)
                .map(IpAddressUtils::getIpAddress)
                .flatMap(Optional::stream);
    }

    @Override
    public Set<IPAddress> getData() {
        return data;
    }
}
