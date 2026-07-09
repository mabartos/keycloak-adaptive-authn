package io.github.mabartos.context.ip.whitelist;

import io.github.mabartos.context.ip.IPAddress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merged, sorted IPv4 ranges with O(log n) membership checks.
 */
public final class IpWhitelistIndex {

    private static final IpWhitelistIndex EMPTY = new IpWhitelistIndex(List.of());

    private final List<IpWhitelistEntry> ranges;

    private IpWhitelistIndex(List<IpWhitelistEntry> ranges) {
        this.ranges = ranges;
    }

    public static IpWhitelistIndex empty() {
        return EMPTY;
    }

    public static IpWhitelistIndex of(List<IpWhitelistEntry> input) {
        if (input == null || input.isEmpty()) {
            return EMPTY;
        }

        List<IpWhitelistEntry> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparing(IpWhitelistEntry::start));

        List<IpWhitelistEntry> merged = new ArrayList<>(sorted.size());
        IpWhitelistEntry current = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            IpWhitelistEntry next = sorted.get(i);
            if (overlaps(current, next)) {
                current = merge(current, next);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return new IpWhitelistIndex(List.copyOf(merged));
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    int size() {
        return ranges.size();
    }

    public boolean contains(IPAddress ip) {
        if (ip == null || ranges.isEmpty()) {
            return false;
        }

        int lo = 0;
        int hi = ranges.size() - 1;
        int candidate = -1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            IpWhitelistEntry range = ranges.get(mid);
            if (ip.compareTo(range.start()) >= 0) {
                candidate = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return candidate >= 0 && ranges.get(candidate).contains(ip);
    }

    private static boolean overlaps(IpWhitelistEntry left, IpWhitelistEntry right) {
        return left.start().compareTo(right.end()) <= 0 && right.start().compareTo(left.end()) <= 0;
    }

    private static IpWhitelistEntry merge(IpWhitelistEntry left, IpWhitelistEntry right) {
        IPAddress mergedStart = left.start().compareTo(right.start()) <= 0 ? left.start() : right.start();
        IPAddress mergedEnd = left.end().compareTo(right.end()) >= 0 ? left.end() : right.end();
        return new IpWhitelistEntry.Range(mergedStart, mergedEnd);
    }
}
