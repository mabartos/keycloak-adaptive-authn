package io.github.mabartos.context.ip.allowlist;

import io.github.mabartos.context.ip.IPAddress;

/**
 * Parsed IPv4 allowlist entry.
 */
sealed interface IpAllowlistEntry permits IpAllowlistEntry.Single, IpAllowlistEntry.Range, IpAllowlistEntry.Cidr {

    boolean contains(IPAddress ip);

    IPAddress start();

    IPAddress end();

    record Single(IPAddress address) implements IpAllowlistEntry {
        @Override
        public boolean contains(IPAddress ip) {
            return ip != null && address.equals(ip);
        }

        @Override
        public IPAddress start() {
            return address;
        }

        @Override
        public IPAddress end() {
            return address;
        }
    }

    record Range(IPAddress start, IPAddress end) implements IpAllowlistEntry {
        @Override
        public boolean contains(IPAddress ip) {
            return ip != null && ip.isInRange(start, end);
        }
    }

    record Cidr(CidrRange range) implements IpAllowlistEntry {
        @Override
        public boolean contains(IPAddress ip) {
            return range.contains(ip);
        }

        @Override
        public IPAddress start() {
            return range.start();
        }

        @Override
        public IPAddress end() {
            return range.end();
        }
    }
}
