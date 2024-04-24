package org.keycloak.adaptive.context.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import org.keycloak.adaptive.context.DeviceContext;
import org.keycloak.representations.account.DeviceRepresentation;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Optional;

public class IpAddressUtils {

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
}
