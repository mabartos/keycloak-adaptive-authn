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
package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpAddressUtils {

    public static final Pattern IP_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
    public static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=([^;]+)");

    public static boolean isInRange(DeviceContext context, String value) {
        if (StringUtil.isBlank(value)) throw new IllegalArgumentException("Cannot parse IP Address");

        return Arrays.stream(value.split(","))
                .filter(f -> f.contains("-"))
                .anyMatch(f -> manageRange(context, f));
    }

    private static boolean manageRange(DeviceContext context, String ipAddress) {
        var items = ipAddress.split("-");
        if (items.length != 2) {
            throw new IllegalArgumentException("Invalid IP Address range format");
        }

        try {
            var start = new IPAddressString(items[0]).getAddress();
            var end = new IPAddressString(items[1]).getAddress();
            var ipRange = start.spanWithRange(end);

            var deviceIp = Optional.ofNullable(context.getData())
                    .map(DeviceRepresentation::getIpAddress)
                    .filter(StringUtil::isNotBlank);

            if (deviceIp.isEmpty()) throw new IllegalArgumentException("Cannot obtain IP Address");

            return ipRange.contains(new IPAddressString(deviceIp.get()).getAddress());
        } catch (IncompatibleAddressException e) {
            throw new IllegalArgumentException("Cannot parse IP Address", e);
        }
    }

    public static Optional<IPAddress> getIpAddress(String ipAddress) {
        try {
            return Optional.ofNullable(new IPAddressString(ipAddress).getAddress());
        } catch (IncompatibleAddressException e) {
            return Optional.empty();
        }
    }

    private static IPAddress parseForwardedHeader(String header, Pattern pattern) {
        Matcher matcher = pattern.matcher(header);
        if (matcher.find()) {
            var ip = IpAddressUtils.getIpAddress(matcher.group(1));
            if (ip.isPresent()) {
                return ip.get();
            }
        }
        return null;
    }

    public static Optional<IPAddress> getIpAddressFromHeader(HttpHeaders headers, String headerName, Pattern pattern) {
        return Optional.ofNullable(headers.getRequestHeader(headerName))
                .flatMap(h -> h.stream().findFirst())
                .map(h -> List.of(h.split(",")))
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(f -> parseForwardedHeader(f, pattern));
    }
}
