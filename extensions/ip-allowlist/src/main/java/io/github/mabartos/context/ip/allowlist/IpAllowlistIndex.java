package io.github.mabartos.context.ip.allowlist;

import io.github.mabartos.context.ip.IPAddress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merged, sorted IPv4 ranges with O(log n) membership checks.
 */
public final class IpAllowlistIndex {

    private static final IpAllowlistIndex EMPTY = new IpAllowlistIndex(List.of());

    private final List<IpAllowlistEntry> ranges;

    private IpAllowlistIndex(List<IpAllowlistEntry> ranges) {
        this.ranges = ranges;
    }

    public static IpAllowlistIndex empty() {
        return EMPTY;
    }

    public static IpAllowlistIndex of(List<IpAllowlistEntry> input) {
        if (input == null || input.isEmpty()) {
            return EMPTY;
        }

        List<IpAllowlistEntry> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparing(IpAllowlistEntry::start));

        List<IpAllowlistEntry> merged = new ArrayList<>(sorted.size());
        IpAllowlistEntry current = sorted.getFirst();
        for (int i = 1; i < sorted.size(); i++) {
            IpAllowlistEntry next = sorted.get(i);
            if (overlaps(current, next)) {
                current = merge(current, next);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return new IpAllowlistIndex(List.copyOf(merged));
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
            IpAllowlistEntry range = ranges.get(mid);
            if (ip.compareTo(range.start()) >= 0) {
                candidate = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return candidate >= 0 && ranges.get(candidate).contains(ip);
    }

    private static boolean overlaps(IpAllowlistEntry left, IpAllowlistEntry right) {
        return left.start().compareTo(right.end()) <= 0 && right.start().compareTo(left.end()) <= 0;
    }

    private static IpAllowlistEntry merge(IpAllowlistEntry left, IpAllowlistEntry right) {
        IPAddress mergedStart = left.start().compareTo(right.start()) <= 0 ? left.start() : right.start();
        IPAddress mergedEnd = left.end().compareTo(right.end()) >= 0 ? left.end() : right.end();
        return new IpAllowlistEntry.Range(mergedStart, mergedEnd);
    }
}
