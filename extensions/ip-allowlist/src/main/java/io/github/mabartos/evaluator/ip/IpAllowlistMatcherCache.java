package io.github.mabartos.evaluator.ip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.mabartos.context.ip.allowlist.IpAllowlistMatcher;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.time.Duration;
import java.util.Optional;

/**
 * JVM-scoped cache of parsed {@link IpAllowlistMatcher} instances per realm allowlist config.
 * <p>
 * Cache keys include the raw allowlist realm attribute so admin updates take effect immediately.
 * TTL and size limits evict stale entries and bound memory.
 */
final class IpAllowlistMatcherCache {
    private static final Logger log = Logger.getLogger(IpAllowlistMatcherCache.class);

    private static final String TTL_PROPERTY = "ip-allowlist.cache.ttl";
    private static final String MAXIMUM_SIZE_PROPERTY = "ip-allowlist.cache.maximum-size";

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final long DEFAULT_MAXIMUM_SIZE = 1_000L;

    private static final Cache<String, Optional<IpAllowlistMatcher>> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(resolveTtl())
            .maximumSize(resolveMaximumSize())
            .removalListener(IpAllowlistMatcherCache::onRemoval)
            .build();

    private IpAllowlistMatcherCache() {
    }

    static Optional<IpAllowlistMatcher> get(RealmModel realm) {
        if (realm == null) {
            return Optional.empty();
        }
        String key = cacheKey(realm);
        return CACHE.get(key, ignored -> buildMatcher(realm));
    }

    static void clear() {
        CACHE.invalidateAll();
    }

    private static Optional<IpAllowlistMatcher> buildMatcher(RealmModel realm) {
        var entries = IpAllowlistEvaluatorConfig.allowlistEntries(realm);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(IpAllowlistMatcher.fromEntries(entries));
    }

    private static String cacheKey(RealmModel realm) {
        String raw = realm.getAttribute(IpAllowlistEvaluatorConfig.ALLOWLIST_IPV4_CONFIG);
        return realm.getId() + ":" + (raw != null ? raw.trim() : "");
    }

    private static Duration resolveTtl() {
        return Configuration.getOptionalValue(TTL_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Duration.parse(value);
                    } catch (RuntimeException e) {
                        log.warnf("Invalid IP allowlist cache TTL '%s', using default %s", value, DEFAULT_TTL);
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
                        log.warnf("Invalid IP allowlist cache maximum size '%s', using default %d",
                                value, DEFAULT_MAXIMUM_SIZE);
                        return DEFAULT_MAXIMUM_SIZE;
                    }
                })
                .orElse(DEFAULT_MAXIMUM_SIZE);
    }

    private static void onRemoval(String key, Optional<IpAllowlistMatcher> matcher, RemovalCause cause) {
        if (key != null && cause.wasEvicted()) {
            log.tracef("Removed IP allowlist cache entry for key=%s due to %s", key, cause);
        }
    }
}
