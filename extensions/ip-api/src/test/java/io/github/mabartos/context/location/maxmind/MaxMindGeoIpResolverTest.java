package io.github.mabartos.context.location.maxmind;

import io.github.mabartos.context.ip.IPAddress;
import io.github.mabartos.context.location.LocationData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MaxMindGeoIpResolverTest {

    @AfterEach
    void tearDown() {
        MaxMindDatabaseManager.getInstance().shutdown();
    }

    @Test
    void resolve_returnsLocationForKnownIp() throws Exception {
        MaxMindTestFixtures.assumeTestDatabasePresent();
        Path testDb = MaxMindTestFixtures.testDatabasePath();
        Path dbPath = Files.createTempFile("geolite2-city", ".mmdb");
        dbPath.toFile().deleteOnExit();
        Files.copy(testDb, dbPath, StandardCopyOption.REPLACE_EXISTING);

        MaxMindDatabaseManager manager = MaxMindDatabaseManager.getInstance();
        manager.dbPath = dbPath;
        manager.openReader(dbPath);

        MaxMindGeoIpResolver resolver = new MaxMindGeoIpResolver(manager);
        IPAddress ip = IPAddress.parse("81.2.69.160");

        Optional<LocationData> data = resolver.resolve(null, null, ip);

        assertThat(data.isPresent(), is(true));
        assertThat(data.get().getCountry(), is("United Kingdom"));
        assertThat(data.get().getCity(), is("London"));
        assertThat(resolver.id(), is("maxmind"));
    }

    @Test
    void resolve_returnsEmptyWhenReaderUnavailable() {
        MaxMindGeoIpResolver resolver = new MaxMindGeoIpResolver(MaxMindDatabaseManager.getInstance());
        Optional<LocationData> data = resolver.resolve(null, null, IPAddress.parse("8.8.8.8"));

        assertThat(data.isPresent(), is(false));
    }
}
