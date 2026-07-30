package io.github.mabartos.context.location.maxmind;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MaxMindDatabaseDownloaderTest {

    @Test
    void resolveSource_defaultsToMirrorWithoutCredentials() {
        assertThat(new MaxMindDatabaseDownloader().resolveSource(), is(MaxMindDownloadSource.MIRROR));
    }

    @Test
    void extractMmdbFromTarGz_writesMmdbEntry(@TempDir Path tempDir) throws IOException {
        MaxMindTestFixtures.assumeTestDatabasePresent();
        Path testDb = MaxMindTestFixtures.testDatabasePath();
        Path tarGz = createTarGzFixture(testDb);
        Path target = tempDir.resolve("GeoLite2-City.mmdb");

        try (var input = Files.newInputStream(tarGz)) {
            MaxMindDatabaseDownloader.extractMmdbFromTarGz(input, target);
        }

        assertThat(MaxMindDatabaseDownloader.validateDatabase(target), is(true));
    }

    @Test
    void validateDatabase_rejectsMissingFile(@TempDir Path tempDir) {
        assertThat(MaxMindDatabaseDownloader.validateDatabase(tempDir.resolve("missing.mmdb")), is(false));
    }

    private static Path createTarGzFixture(Path mmdbSource) throws IOException {
        Path tarGz = Files.createTempFile("geolite2-city", ".tar.gz");
        byte[] content = Files.readAllBytes(mmdbSource);
        try (OutputStream fileOut = Files.newOutputStream(tarGz);
                GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut);
                TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {
            TarArchiveEntry entry = new TarArchiveEntry("GeoLite2-City_20260101/GeoLite2-City.mmdb");
            entry.setSize(content.length);
            tarOut.putArchiveEntry(entry);
            tarOut.write(content);
            tarOut.closeArchiveEntry();
        }
        return tarGz;
    }
}
