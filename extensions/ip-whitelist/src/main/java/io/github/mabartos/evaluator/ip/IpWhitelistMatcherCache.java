package io.github.mabartos.evaluator.ip;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.mabartos.context.ip.whitelist.IpWhitelistMatcher;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.time.Duration;
import java.util.Optional;

/**
 * JVM-scoped cache of parsed {@link IpWhitelistMatcher} instances per realm whitelist config.
 * <p>
 * Cache keys include the raw whitelist realm attribute so admin updates take effect immediately.
 * TTL and size limits evict stale entries and bound memory.
 */
final class IpWhitelistMatcherCache {
    private static final Logger log = Logger.getLogger(IpWhitelistMatcherCache.class);

    private static final String TTL_PROPERTY = "ip-whitelist.cache.ttl";
    private static final String MAXIMUM_SIZE_PROPERTY = "ip-whitelist.cache.maximum-size";

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final long DEFAULT_MAXIMUM_SIZE = 1_000L;

    private static final Cache<String, Optional<IpWhitelistMatcher>> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(resolveTtl())
            .maximumSize(resolveMaximumSize())
            .removalListener(IpWhitelistMatcherCache::onRemoval)
            .build();

    private IpWhitelistMatcherCache() {
    }

    static Optional<IpWhitelistMatcher> get(RealmModel realm) {
        if (realm == null) {
            return Optional.empty();
        }
        String key = cacheKey(realm);
        return CACHE.get(key, ignored -> buildMatcher(realm));
    }

    static void clear() {
        CACHE.invalidateAll();
    }

    private static Optional<IpWhitelistMatcher> buildMatcher(RealmModel realm) {
        var entries = IpWhitelistEvaluatorConfig.whitelistEntries(realm);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(IpWhitelistMatcher.fromEntries(entries));
    }

    private static String cacheKey(RealmModel realm) {
        String raw = realm.getAttribute(IpWhitelistEvaluatorConfig.WHITELIST_IPV4_CONFIG);
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
                        log.warnf("Invalid IP whitelist cache TTL '%s', using default %s", value, DEFAULT_TTL);
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
                        log.warnf("Invalid IP whitelist cache maximum size '%s', using default %d",
                                value, DEFAULT_MAXIMUM_SIZE);
                        return DEFAULT_MAXIMUM_SIZE;
                    }
                })
                .orElse(DEFAULT_MAXIMUM_SIZE);
    }

    private static void onRemoval(String key, Optional<IpWhitelistMatcher> matcher, RemovalCause cause) {
        if (key != null && cause.wasEvicted()) {
            log.tracef("Removed IP whitelist cache entry for key=%s due to %s", key, cause);
        }
    }
}
