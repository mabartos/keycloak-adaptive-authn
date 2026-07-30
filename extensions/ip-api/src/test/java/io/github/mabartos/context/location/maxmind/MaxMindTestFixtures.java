package io.github.mabartos.context.location.maxmind;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the official MaxMind test database downloaded at build time
 * ({@code generate-test-resources}) into {@code target/test-classes/maxmind/}.
 */
final class MaxMindTestFixtures {

    private static final String TEST_DB_RESOURCE = "/maxmind/GeoLite2-City-Test.mmdb";

    private MaxMindTestFixtures() {
    }

    static Path testDatabasePath() throws IOException {
        var resource = MaxMindTestFixtures.class.getResource(TEST_DB_RESOURCE);
        if (resource == null) {
            throw new org.opentest4j.TestAbortedException(
                    "GeoLite2-City-Test.mmdb not on classpath; run mvn test (downloads from MaxMind-DB)");
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid test database URI", e);
        }
    }

    static void assumeTestDatabasePresent() throws IOException {
        if (!Files.isRegularFile(testDatabasePath())) {
            throw new org.opentest4j.TestAbortedException("Test database missing at " + TEST_DB_RESOURCE);
        }
    }
}
