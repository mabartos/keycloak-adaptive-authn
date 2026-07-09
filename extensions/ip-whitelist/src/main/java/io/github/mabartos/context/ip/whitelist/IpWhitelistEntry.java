package io.github.mabartos.context.ip.whitelist;

import io.github.mabartos.context.ip.IPAddress;

/**
 * Parsed IPv4 whitelist entry.
 */
sealed interface IpWhitelistEntry permits IpWhitelistEntry.Single, IpWhitelistEntry.Range, IpWhitelistEntry.Cidr {

    boolean contains(IPAddress ip);

    IPAddress start();

    IPAddress end();

    record Single(IPAddress address) implements IpWhitelistEntry {
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

    record Range(IPAddress start, IPAddress end) implements IpWhitelistEntry {
        @Override
        public boolean contains(IPAddress ip) {
            return ip != null && ip.isInRange(start, end);
        }
    }

    record Cidr(CidrRange range) implements IpWhitelistEntry {
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
