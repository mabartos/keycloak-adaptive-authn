package io.github.mabartos.context.location;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.mabartos.context.ip.IPAddress;
import org.jboss.logging.Logger;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.time.Duration;
import java.util.Optional;

final class GlobalLocationCache {
    private static final Logger log = Logger.getLogger(GlobalLocationCache.class);

    private static final String TTL_PROPERTY = "location.global-cache.ttl";
    private static final String MAXIMUM_SIZE_PROPERTY = "location.global-cache.maximum-size";

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final long DEFAULT_MAXIMUM_SIZE = 10_000L;

    private static final Cache<String, LocationData> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(resolveTtl())
            .maximumSize(resolveMaximumSize())
            .removalListener(GlobalLocationCache::onRemoval)
            .build();

    private GlobalLocationCache() {
    }

    static Optional<LocationData> get(IPAddress ip) {
        return Optional.ofNullable(CACHE.getIfPresent(ip.toString()));
    }

    static void put(IPAddress ip, LocationData location) {
        String ipString = ip.toString();
        CACHE.put(ipString, location);
        log.tracef("Updated global location cache: IP=%s", ipString);
    }

    private static Duration resolveTtl() {
        return Configuration.getOptionalValue(TTL_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Duration.parse(value);
                    } catch (RuntimeException e) {
                        log.warnf("Invalid global location cache TTL '%s', using default %s", value, DEFAULT_TTL);
                        return DEFAULT_TTL;
                    }
                })
                .orElse(DEFAULT_TTL);
    }

    private static long resolveMaximumSize() {
        return Configuration.getOptionalValue(MAXIMUM_SIZE_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        long maximumSize = Long.parseLong(value);
                        if (maximumSize <= 0) {
                            throw new IllegalArgumentException("Maximum size must be positive");
                        }
                        return maximumSize;
                    } catch (RuntimeException e) {
                        log.warnf("Invalid global location cache maximum size '%s', using default %d",
                                value, DEFAULT_MAXIMUM_SIZE);
                        return DEFAULT_MAXIMUM_SIZE;
                    }
                })
                .orElse(DEFAULT_MAXIMUM_SIZE);
    }

    private static void onRemoval(String ip, LocationData location, RemovalCause cause) {
        if (ip != null && cause.wasEvicted()) {
            log.tracef("Removed global location cache entry for IP=%s due to %s", ip, cause);
        }
    }
}