package org.example.reading.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolved configuration for the local reading shelf application.
 */
public final class ReadingShelfConfig {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_DATA_PATH_VALUE = "data/runtime/reading-shelf.json";
    public static final Path DEFAULT_DATA_PATH =
            Paths.get(DEFAULT_DATA_PATH_VALUE).toAbsolutePath().normalize();

    public static final String PORT_PROPERTY = "reading.shelf.port";
    public static final String DATA_PATH_PROPERTY = "reading.shelf.data.path";

    private static final Path LEGACY_DATA_PATH = Paths.get("data/reading-shelf.json");

    private final int port;
    private final Path dataPath;

    /**
     * Resolves process configuration.
     */
    public ReadingShelfConfig() {
        this(resolvePort(), resolveDataPath());
    }

    /**
     * Creates test/process-independent configuration.
     */
    public ReadingShelfConfig(int port, Path dataPath) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
        this.dataPath = validateDataPath(dataPath);
    }

    public ReadingShelfConfig(int port, String dataPath) {
        this(port, parseDataPath(dataPath));
    }

    public String getHost() {
        return DEFAULT_HOST;
    }

    public String host() {
        return getHost();
    }

    public int getPort() {
        return port;
    }

    public int port() {
        return port;
    }

    public Path getDataPath() {
        return dataPath;
    }

    public Path dataPath() {
        return dataPath;
    }

    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    private static int resolvePort() {
        try {
            String value = System.getProperty(PORT_PROPERTY);
            if (value == null) {
                return DEFAULT_PORT;
            }
            if (!value.equals(value.trim()) || value.isEmpty()) {
                throw new IllegalArgumentException("Invalid reading.shelf.port property");
            }
            int port = Integer.parseInt(value);
            if (!isValidPort(port)) {
                throw new IllegalArgumentException("Invalid reading.shelf.port property");
            }
            return port;
        } catch (SecurityException exception) {
            throw new IllegalArgumentException("Unable to read reading.shelf.port property", exception);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid reading.shelf.port property", exception);
        }
    }

    private static Path resolveDataPath() {
        try {
            String value = System.getProperty(DATA_PATH_PROPERTY);
            if (value == null) {
                return DEFAULT_DATA_PATH;
            }
            if (!value.equals(value.trim()) || value.isEmpty()) {
                throw new IllegalArgumentException("Invalid reading.shelf.data.path property");
            }
            Path candidate = Paths.get(value).toAbsolutePath().normalize();
            if (isForbiddenLegacyPath(candidate)) {
                throw new IllegalArgumentException("Legacy data path is not supported");
            }
            return candidate;
        } catch (java.nio.file.InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid reading.shelf.data.path property", exception);
        } catch (SecurityException exception) {
            throw new IllegalArgumentException("Unable to read reading.shelf.data.path property", exception);
        }
    }

    private static Path parseDataPath(String dataPath) {
        if (dataPath == null || dataPath.isEmpty()
                || dataPath.trim().isEmpty() || !dataPath.equals(dataPath.trim())) {
            throw new IllegalArgumentException("Data path must not be blank");
        }
        try {
            return Paths.get(dataPath).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Data path is invalid", exception);
        }
    }

    private static Path validateDataPath(Path dataPath) {
        if (dataPath == null) {
            throw new IllegalArgumentException("Data path must not be null");
        }
        if (dataPath.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Data path must not be blank");
        }
        Path normalized = dataPath.toAbsolutePath().normalize();
        if (isForbiddenLegacyPath(normalized)) {
            throw new IllegalArgumentException("Legacy data path is not supported");
        }
        return normalized;
    }

    private static boolean isForbiddenLegacyPath(Path candidate) {
        Path normalizedCandidate = candidate.normalize();
        Path normalizedLegacy = LEGACY_DATA_PATH.normalize();
        if (normalizedCandidate.equals(normalizedLegacy)) {
            return true;
        }
        try {
            Path absoluteCandidate = normalizedCandidate.toAbsolutePath().normalize();
            Path absoluteLegacy = LEGACY_DATA_PATH.toAbsolutePath().normalize();
            return absoluteCandidate.equals(absoluteLegacy);
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
