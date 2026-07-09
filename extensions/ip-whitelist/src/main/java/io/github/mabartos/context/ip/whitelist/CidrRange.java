package io.github.mabartos.context.ip.whitelist;

import io.github.mabartos.context.ip.IPAddress;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * IPv4 CIDR range for whitelist membership checks.
 */
final class CidrRange {

    private final IPAddress start;
    private final IPAddress end;

    CidrRange(IPAddress start, IPAddress end) {
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
    }

    IPAddress start() {
        return start;
    }

    IPAddress end() {
        return end;
    }

    boolean contains(IPAddress ip) {
        return ip != null && ip.isInRange(start, end);
    }

    boolean overlaps(CidrRange other) {
        return start.compareTo(other.end) <= 0 && other.start.compareTo(end) <= 0;
    }

    CidrRange merge(CidrRange other) {
        IPAddress mergedStart = start.compareTo(other.start) <= 0 ? start : other.start;
        IPAddress mergedEnd = end.compareTo(other.end) >= 0 ? end : other.end;
        return new CidrRange(mergedStart, mergedEnd);
    }

    static CidrRange parseIpv4(String cidr) {
        if (cidr == null || cidr.isBlank()) {
            return null;
        }

        int slash = cidr.indexOf('/');
        if (slash <= 0 || slash >= cidr.length() - 1) {
            return null;
        }

        String networkPart = cidr.substring(0, slash).trim();
        String prefixPart = cidr.substring(slash + 1).trim();

        int prefix;
        try {
            prefix = Integer.parseInt(prefixPart);
        } catch (NumberFormatException e) {
            return null;
        }

        IPAddress network = IPAddress.parse(networkPart);
        if (network == null || !network.isIPv4()) {
            return null;
        }
        if (prefix < 0 || prefix > 32) {
            return null;
        }

        try {
            byte[] networkBytes = InetAddress.getByName(networkPart).getAddress();
            BigInteger networkValue = new BigInteger(1, networkBytes);
            int hostBits = 32 - prefix;
            BigInteger hostMask = hostBits == 32
                    ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE);
            BigInteger networkMask = hostMask.not().and(BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE));

            BigInteger startValue = networkValue.and(networkMask);
            BigInteger endValue = startValue.or(hostMask);

            IPAddress start = toIpv4(startValue);
            IPAddress end = toIpv4(endValue);
            if (start == null || end == null) {
                return null;
            }
            return new CidrRange(start, end);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static IPAddress toIpv4(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] normalized = new byte[4];
        int copyStart = Math.max(0, bytes.length - 4);
        int copyLength = Math.min(bytes.length, 4);
        System.arraycopy(bytes, copyStart, normalized, 4 - copyLength, copyLength);
        try {
            return IPAddress.parse(InetAddress.getByAddress(normalized).getHostAddress());
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
