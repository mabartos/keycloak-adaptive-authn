/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adaptive.context.ip.proxy;

import inet.ipaddr.IPAddress;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.adaptive.context.ip.IpAddressUtils;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.StringUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.adaptive.context.ip.IpAddressUtils.IP_PATTERN;

public class ProxyIpAddressContext extends IpProxyContext {
    private final KeycloakSession session;

    public ProxyIpAddressContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public Optional<Set<IPAddress>> initData() {
        return Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getRequestHeaders)
                .map(headers -> Stream.concat(
                        getIpAddressFromHeader(headers, "Forwarded"),
                        getIpAddressFromHeader(headers, "X-Forwarded-For"))
                )
                .map(f -> f.collect(Collectors.toSet()));
    }

    protected static Stream<IPAddress> getIpAddressFromHeader(HttpHeaders headers, String headerName) {
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
}
