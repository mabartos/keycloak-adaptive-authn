package io.github.mabartos.context.ip.allowlist;

import io.github.mabartos.context.ip.IPAddress;

import java.util.Objects;

/**
 * Inclusive IPv4 interval. Membership uses core {@link IPAddress#isInRange(IPAddress, IPAddress)}.
 */
record IpAllowlistEntry(IPAddress start, IPAddress end) {

    IpAllowlistEntry {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
    }

    static IpAllowlistEntry of(IPAddress start, IPAddress end) {
        return new IpAllowlistEntry(start, end);
    }

    static IpAllowlistEntry single(IPAddress address) {
        return new IpAllowlistEntry(address, address);
    }

    boolean contains(IPAddress ip) {
        return ip != null && ip.isInRange(start, end);
    }
}
