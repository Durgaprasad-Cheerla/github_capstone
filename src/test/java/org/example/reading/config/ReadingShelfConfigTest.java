package org.example.reading.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadingShelfConfigTest {
    private final String originalPort = System.getProperty(ReadingShelfConfig.PORT_PROPERTY);
    private final String originalDataPath = System.getProperty(ReadingShelfConfig.DATA_PATH_PROPERTY);

    @AfterEach
    void restoreSystemProperties() {
        restoreProperty(ReadingShelfConfig.PORT_PROPERTY, originalPort);
        restoreProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, originalDataPath);
    }

    @Test
    @DisplayName("FR-06, FR-07, NFR-01: defaults resolve to loopback runtime configuration")
    void resolvesDefaults() {
        clearProperty(ReadingShelfConfig.PORT_PROPERTY);
        clearProperty(ReadingShelfConfig.DATA_PATH_PROPERTY);

        ReadingShelfConfig config = new ReadingShelfConfig();

        assertEquals("127.0.0.1", config.host());
        assertEquals(8080, config.port());
        assertEquals(ReadingShelfConfig.DEFAULT_DATA_PATH, config.dataPath());
    }

    @Test
    @DisplayName("NFR-04: test constructor parameters avoid process-global state")
    void acceptsInjectedValues() {
        ReadingShelfConfig config =
                new ReadingShelfConfig(9090, Path.of("target/test-data/shelf.json"));

        assertEquals("127.0.0.1", config.host());
        assertEquals(9090, config.port());
        assertEquals(Path.of("target/test-data/shelf.json").toAbsolutePath().normalize(), config.dataPath());
    }

    @Test
    @DisplayName("FR-06, FR-07: valid JVM property overrides are honored")
    void honorsValidOverrides() {
        System.setProperty(ReadingShelfConfig.PORT_PROPERTY, "9091");
        System.setProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, "target/test-data/custom.json");

        ReadingShelfConfig config = new ReadingShelfConfig();

        assertEquals(9091, config.port());
        assertEquals(
                Path.of("target/test-data/custom.json").toAbsolutePath().normalize(),
                config.dataPath());
    }

    @Test
    @DisplayName("FR-06, NFR-01: invalid JVM property overrides fall back to defaults")
    void rejectsInvalidOverrides() {
        System.setProperty(ReadingShelfConfig.PORT_PROPERTY, "70000");
        System.setProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, "data/reading-shelf.json");

        assertThrows(IllegalArgumentException.class, ReadingShelfConfig::new);
    }

    @Test
    @DisplayName("FR-07: malformed port and blank path overrides are rejected")
    void rejectsMalformedOverrides() {
        System.setProperty(ReadingShelfConfig.PORT_PROPERTY, "not-a-port");
        System.setProperty(ReadingShelfConfig.DATA_PATH_PROPERTY, " ");

        assertThrows(IllegalArgumentException.class, ReadingShelfConfig::new);
    }

    @Test
    @DisplayName("NFR-04: test constructor rejects invalid port and legacy data path")
    void rejectsInvalidInjectedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingShelfConfig(0, Path.of("target/test-data/shelf.json")));
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingShelfConfig(8080, Path.of("data/reading-shelf.json")));
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingShelfConfig(8080, (Path) null));
    }

    private static void clearProperty(String key) {
        System.clearProperty(key);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
