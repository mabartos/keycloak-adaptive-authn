package io.github.mabartos.context.ip.whitelist;

import io.github.mabartos.context.ip.IPAddress;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses whitelist entry strings and performs IPv4 membership checks.
 */
public final class IpWhitelistMatcher {

    private static final Logger logger = Logger.getLogger(IpWhitelistMatcher.class);

    private final IpWhitelistIndex index;

    private IpWhitelistMatcher(IpWhitelistIndex index) {
        this.index = index;
    }

    public static IpWhitelistMatcher fromEntries(List<String> entries) {
        List<IpWhitelistEntry> parsed = new ArrayList<>();
        for (String entry : entries) {
            parseEntry(entry).ifPresent(parsed::add);
        }
        return new IpWhitelistMatcher(IpWhitelistIndex.of(parsed));
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public boolean contains(IPAddress ip) {
        IPAddress normalized = normalizeClientIp(ip);
        if (normalized == null) {
            return false;
        }
        return index.contains(normalized);
    }

    public static boolean isValidEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        return parseEntry(entry).isPresent();
    }

    static Optional<IpWhitelistEntry> parseEntry(String entry) {
        if (entry == null) {
            return Optional.empty();
        }
        var trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (trimmed.contains("/")) {
            CidrRange cidr = CidrRange.parseIpv4(trimmed);
            if (cidr == null) {
                logger.warnf("Ignoring invalid IPv4 CIDR whitelist entry: %s", entry);
                return Optional.empty();
            }
            return Optional.of(new IpWhitelistEntry.Cidr(cidr));
        }

        if (trimmed.contains("-")) {
            var parts = trimmed.split("-", 2);
            if (parts.length != 2) {
                logger.warnf("Ignoring invalid IPv4 range whitelist entry: %s", entry);
                return Optional.empty();
            }
            IPAddress start = IPAddress.parse(parts[0].trim());
            IPAddress end = IPAddress.parse(parts[1].trim());
            if (!isIpv4(start) || !isIpv4(end) || start.compareTo(end) > 0) {
                logger.warnf("Ignoring invalid IPv4 range whitelist entry: %s", entry);
                return Optional.empty();
            }
            return Optional.of(new IpWhitelistEntry.Range(start, end));
        }

        IPAddress single = IPAddress.parse(trimmed);
        if (!isIpv4(single)) {
            logger.warnf("Ignoring invalid IPv4 whitelist entry: %s", entry);
            return Optional.empty();
        }
        return Optional.of(new IpWhitelistEntry.Single(single));
    }

    static IPAddress normalizeClientIp(IPAddress ip) {
        if (ip == null) {
            return null;
        }
        if (ip.isIPv4()) {
            return ip;
        }
        if (ip.isIPv4Convertible()) {
            String host = ip.toString();
            if (host.contains(".")) {
                return IPAddress.parse(host);
            }
            int lastColon = host.lastIndexOf(':');
            if (lastColon >= 0 && host.indexOf('.', lastColon) > lastColon) {
                return IPAddress.parse(host.substring(lastColon + 1));
            }
            if (host.regionMatches(true, 0, "::ffff:", 0, 7)) {
                return IPAddress.parse(host.substring(7));
            }
        }
        return null;
    }

    private static boolean isIpv4(IPAddress ip) {
        return ip != null && ip.isIPv4();
    }
}
