package io.github.mabartos.context.location;

import inet.ipaddr.IPAddress;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class GlobalLocationCache {
    private static final Logger log = Logger.getLogger(GlobalLocationCache.class);

    private static final long TTL_MILLIS = Duration.ofHours(24).toMillis();
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private GlobalLocationCache() {
    }

    static Optional<LocationData> get(IPAddress ip) {
        String ipString = ip.toString();
        CacheEntry entry = CACHE.get(ipString);
        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            CACHE.remove(ipString, entry);
            log.tracef("Expired global location cache entry removed for IP=%s", ipString);
            return Optional.empty();
        }

        return Optional.of(entry.location());
    }

    static void put(IPAddress ip, LocationData location) {
        String ipString = ip.toString();
        CACHE.put(ipString, new CacheEntry(location, System.currentTimeMillis() + TTL_MILLIS));
        log.tracef("Updated global location cache: IP=%s", ipString);
    }

    private record CacheEntry(LocationData location, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}