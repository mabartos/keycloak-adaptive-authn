package io.github.mabartos.context.location.maxmind;

import com.maxmind.geoip2.DatabaseReader;
import io.github.mabartos.context.location.geoip.AdaptiveConfig;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Downloads GeoLite2-City from MaxMind (official API) or a community mirror.
 */
public final class MaxMindDatabaseDownloader {

    static final String OFFICIAL_URL =
            "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz";

    static final String MIRROR_URL =
            "https://github.com/P3TERX/GeoLite.mmdb/raw/download/GeoLite2-City.mmdb";

    private static final Logger log = Logger.getLogger(MaxMindDatabaseDownloader.class);

    private final HttpClient httpClient;

    public MaxMindDatabaseDownloader() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    MaxMindDatabaseDownloader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Resolves the download source from configured credentials.
     */
    public MaxMindDownloadSource resolveSource() {
        if (AdaptiveConfig.maxMindOfficialCredentialsPresent()) {
            return MaxMindDownloadSource.OFFICIAL;
        }
        if (AdaptiveConfig.maxMindPartialCredentialsPresent()) {
            log.warnf(
                    "Both %s and %s must be set for official MaxMind downloads; "
                            + "falling back to community mirror (POC/dev only).",
                    AdaptiveConfig.MAXMIND_ACCOUNT_ID_PROPERTY,
                    AdaptiveConfig.MAXMIND_LICENSE_KEY_PROPERTY);
        } else {
            log.warn(
                    "MaxMind credentials not configured; downloading GeoLite2-City from community mirror "
                            + "(POC/dev only — use official credentials in production per GeoLite2 EULA).");
        }
        return MaxMindDownloadSource.MIRROR;
    }

    /**
     * Downloads the database to {@code targetPath} (parent directories are created).
     *
     * @return {@code true} when a valid database file was written
     */
    public boolean download(Path targetPath, MaxMindDownloadSource source) {
        Path parent = targetPath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                log.errorf(e, "Failed to create MaxMind database directory %s", parent);
                return false;
            }
        }

        Path tmpPath = Path.of(targetPath.toString() + ".tmp");
        try {
            if (source == MaxMindDownloadSource.OFFICIAL) {
                downloadOfficial(tmpPath);
            } else {
                downloadMirror(tmpPath);
            }
            if (!validateDatabase(tmpPath)) {
                Files.deleteIfExists(tmpPath);
                return false;
            }
            Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debugf(
                    "MaxMind GeoLite2-City database updated from %s (%s bytes) at %s",
                    source,
                    Files.size(targetPath),
                    targetPath);
            return true;
        } catch (IOException | InterruptedException e) {
            log.errorf(e, "MaxMind database download failed (source=%s)", source);
            return false;
        } finally {
            try {
                Files.deleteIfExists(tmpPath);
            } catch (IOException e) {
                log.warnf(e, "Failed to delete temporary MaxMind database %s", tmpPath);
            }
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void downloadOfficial(Path targetPath) throws IOException, InterruptedException {
        String accountId = AdaptiveConfig.maxMindAccountId().orElseThrow();
        String licenseKey = AdaptiveConfig.maxMindLicenseKey().orElseThrow();
        String basicAuth = Base64.getEncoder()
                .encodeToString((accountId + ":" + licenseKey).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OFFICIAL_URL))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (status != 200) {
            response.body().close();
            throw new IOException("MaxMind official download returned HTTP " + status);
        }

        try (InputStream body = response.body()) {
            extractMmdbFromTarGz(body, targetPath);
        }
    }

    private void downloadMirror(Path targetPath) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MIRROR_URL))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (status != 200) {
            response.body().close();
            throw new IOException("MaxMind mirror download returned HTTP " + status);
        }

        try (InputStream body = response.body()) {
            Files.copy(body, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void extractMmdbFromTarGz(InputStream gzipStream, Path targetPath) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(gzipStream);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().endsWith("GeoLite2-City.mmdb")) {
                    Files.copy(tar, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("GeoLite2-City.mmdb not found in MaxMind archive");
    }

    static boolean validateDatabase(Path databasePath) {
        try {
            if (!Files.isRegularFile(databasePath) || Files.size(databasePath) == 0) {
                log.warnf("MaxMind database validation failed: empty or missing file at %s", databasePath);
                return false;
            }
            try (DatabaseReader reader = new DatabaseReader.Builder(databasePath.toFile()).build()) {
                reader.metadata();
                return true;
            }
        } catch (IOException e) {
            log.warnf(e, "MaxMind database validation failed for %s", databasePath);
            return false;
        }
    }

    static Optional<Long> fileAgeMillis(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return Optional.empty();
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            return Optional.of(Math.max(0L, System.currentTimeMillis() - modified));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
