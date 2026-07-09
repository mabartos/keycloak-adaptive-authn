package io.github.mabartos.context.location.maxmind;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MaxMindDatabaseManagerTest {

    @AfterEach
    void tearDown() {
        MaxMindDatabaseManager.getInstance().shutdown();
    }

    @Test
    void openReader_loadsExistingDatabase(@TempDir Path tempDir) throws Exception {
        MaxMindTestFixtures.assumeTestDatabasePresent();
        Path testDb = MaxMindTestFixtures.testDatabasePath();
        Path dbPath = tempDir.resolve("GeoLite2-City.mmdb");
        Files.copy(testDb, dbPath, StandardCopyOption.REPLACE_EXISTING);

        MaxMindDatabaseManager manager = MaxMindDatabaseManager.getInstance();
        manager.dbPath = dbPath;
        manager.refreshInterval = Duration.ofDays(7);
        manager.openReader(dbPath);

        assertThat(manager.getReader().isPresent(), is(true));
    }

    @Test
    void refreshIfStale_skipsDownloadWhenDatabaseIsFresh(@TempDir Path tempDir) throws Exception {
        MaxMindTestFixtures.assumeTestDatabasePresent();
        Path testDb = MaxMindTestFixtures.testDatabasePath();
        Path dbPath = tempDir.resolve("GeoLite2-City.mmdb");
        Files.copy(testDb, dbPath, StandardCopyOption.REPLACE_EXISTING);

        MaxMindDatabaseManager manager = MaxMindDatabaseManager.getInstance();
        manager.dbPath = dbPath;
        manager.refreshInterval = Duration.ofDays(7);
        manager.readerRef.set(new DatabaseReader.Builder(dbPath.toFile()).withCache(new CHMCache()).build());

        manager.refreshIfStale();

        assertThat(manager.getReader().isPresent(), is(true));
        assertThat(Files.size(dbPath), is(Files.size(testDb)));
    }
}
