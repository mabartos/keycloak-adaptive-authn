package io.github.mabartos.context.location.maxmind;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import io.github.mabartos.context.location.geoip.AdaptiveConfig;
import io.github.mabartos.context.location.geoip.GeoIpResolverChain;
import io.github.mabartos.context.location.geoip.GeoIpResolverIds;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JVM-wide GeoLite2-City lifecycle: download, periodic refresh, and thread-safe {@link DatabaseReader} access.
 */
public final class MaxMindDatabaseManager {

    private static final Logger log = Logger.getLogger(MaxMindDatabaseManager.class);

    private static final MaxMindDatabaseManager INSTANCE = new MaxMindDatabaseManager();

    /** Grace period before closing a replaced reader so in-flight lookups can finish. */
    private static final Duration READER_CLOSE_DELAY = Duration.ofMinutes(1);

    final AtomicReference<DatabaseReader> readerRef = new AtomicReference<>();
    private final Set<DatabaseReader> retiredReaders = ConcurrentHashMap.newKeySet();
    private final MaxMindDatabaseDownloader downloader = new MaxMindDatabaseDownloader();

    volatile Path dbPath;
    volatile Duration refreshInterval;
    private volatile ScheduledExecutorService scheduler;

    private MaxMindDatabaseManager() {
    }

    public static MaxMindDatabaseManager getInstance() {
        return INSTANCE;
    }

    /**
     * Starts the manager when {@code maxmind} is listed in location providers.
     */
    public synchronized void startIfEnabled() {
        if (!isMaxMindConfigured()) {
            log.debug("MaxMind GeoIP resolver not configured; skipping database manager startup.");
            return;
        }
        if (scheduler != null) {
            return;
        }

        dbPath = AdaptiveConfig.maxMindDbPath();
        refreshInterval = AdaptiveConfig.maxMindDbRefreshInterval();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "maxmind-geolite2-refresh");
            thread.setDaemon(true);
            return thread;
        });

        refreshIfStale();
        long intervalMs = refreshInterval.toMillis();
        scheduler.scheduleWithFixedDelay(this::refreshIfStale, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.infof(
                "MaxMind database manager started (path=%s, refreshInterval=%s)",
                dbPath,
                refreshInterval);
    }

    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        closeReader(readerRef.getAndSet(null));
        for (DatabaseReader retired : retiredReaders) {
            closeReader(retired);
        }
        retiredReaders.clear();
    }

    public Optional<DatabaseReader> getReader() {
        return Optional.ofNullable(readerRef.get());
    }

    void refreshIfStale() {
        if (dbPath == null) {
            dbPath = AdaptiveConfig.maxMindDbPath();
        }
        if (refreshInterval == null) {
            refreshInterval = AdaptiveConfig.maxMindDbRefreshInterval();
        }

        boolean missing = !Files.isRegularFile(dbPath);
        boolean stale = MaxMindDatabaseDownloader.fileAgeMillis(dbPath)
                .map(age -> age > refreshInterval.toMillis())
                .orElse(true);

        if (missing || stale) {
            refresh();
        } else if (readerRef.get() == null) {
            openReader(dbPath);
        }
    }

    void refresh() {
        MaxMindDownloadSource source = downloader.resolveSource();
        log.debugf("Refreshing MaxMind GeoLite2-City database (source=%s, path=%s)", source, dbPath);
        if (downloader.download(dbPath, source)) {
            openReader(dbPath);
        } else if (readerRef.get() == null && Files.isRegularFile(dbPath)) {
            log.warn("MaxMind download failed; attempting to use existing database file.");
            openReader(dbPath);
        }
    }

    void openReader(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            DatabaseReader newReader = new DatabaseReader.Builder(path.toFile())
                    .withCache(new CHMCache())
                    .build();
            DatabaseReader previous = readerRef.getAndSet(newReader);
            deferClose(previous);
            log.debugf("MaxMind DatabaseReader opened for %s", path);
        } catch (IOException e) {
            log.errorf(e, "Failed to open MaxMind database at %s", path);
        }
    }

    private void deferClose(DatabaseReader reader) {
        if (reader == null) {
            return;
        }
        retiredReaders.add(reader);
        ScheduledExecutorService executor = scheduler;
        if (executor == null || executor.isShutdown()) {
            closeRetired(reader);
            return;
        }
        executor.schedule(() -> closeRetired(reader), READER_CLOSE_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void closeRetired(DatabaseReader reader) {
        if (reader != null && retiredReaders.remove(reader)) {
            closeReader(reader);
        }
    }

    private static void closeReader(DatabaseReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Failed to close MaxMind DatabaseReader", e);
            }
        }
    }

    private static boolean isMaxMindConfigured() {
        return GeoIpResolverChain.isProviderConfigured(GeoIpResolverIds.MAXMIND);
    }
}
