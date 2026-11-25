package org.example.reading.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.reading.config.ReadingShelfConfig;
import org.example.reading.shelf.Book;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jackson-backed durable persistence for the reading shelf inventory.
 */
public final class JsonReadingShelfPersistence {
    private final Path dataPath;
    private final ObjectMapper objectMapper;
    private final ReadingShelfIo io;
    private volatile boolean failClosed;

    public JsonReadingShelfPersistence(ReadingShelfConfig config) {
        this(Objects.requireNonNull(config, "config must not be null").dataPath());
    }

    public JsonReadingShelfPersistence(Path dataPath) {
        this(dataPath, new ObjectMapper());
    }

    public JsonReadingShelfPersistence(Path dataPath, ObjectMapper objectMapper) {
        this(dataPath, objectMapper, ReadingShelfIo.nio());
    }

    public JsonReadingShelfPersistence(Path dataPath, ReadingShelfIo io) {
        this(dataPath, new ObjectMapper(), io);
    }

    public JsonReadingShelfPersistence(Path dataPath, ObjectMapper objectMapper, ReadingShelfIo io) {
        if (dataPath == null) {
            throw new IllegalArgumentException("Data path must not be null");
        }
        this.dataPath = dataPath.toAbsolutePath().normalize();
        this.objectMapper = configuredMapper(objectMapper);
        this.io = Objects.requireNonNull(io, "io must not be null");
    }

    private static ObjectMapper configuredMapper(ObjectMapper supplied) {
        if (supplied == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        }
        return supplied.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Path getDataPath() {
        return dataPath;
    }

    public Path dataPath() {
        return dataPath;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    /**
     * Loads the inventory. A missing file is empty; malformed or unreadable
     * existing files are explicit failures and are never overwritten.
     */
    public List<Book> load() throws IOException {
        if (failClosed) {
            throw new IOException("PERSISTENCE_ERROR: Persistence is fail-closed");
        }

        final boolean exists;
        try {
            exists = io.exists(dataPath);
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Unable to read runtime JSON: " + dataPath, exception);
        }
        if (!exists) {
            return new ArrayList<>();
        }

        final byte[] bytes;
        try {
            bytes = io.readAllBytes(dataPath);
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Unable to read runtime JSON: " + dataPath, exception);
        }

        final ShelfDocument document;
        try {
            document = objectMapper.readValue(bytes, ShelfDocument.class);
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new IOException("Malformed runtime JSON: " + dataPath, exception);
        }

        if (document == null || document.books() == null) {
            throw new IOException("Malformed runtime JSON: " + dataPath
                    + " (missing required field \"books\")");
        }
        return new ArrayList<>(document.books());
    }

    public List<Book> loadBooks() throws IOException {
        return load();
    }

    /**
     * Atomically replaces the runtime file after forcing the temporary file.
     * Parent-directory synchronization must complete before success is returned.
     */
    public void save(List<Book> books) throws IOException {
        saveWithResult(books);
    }

    public SaveResult saveWithResult(List<Book> books) throws IOException {
        if (failClosed) {
            throw new IOException("PERSISTENCE_ERROR: Persistence is fail-closed");
        }
        if (books == null) {
            throw new IllegalArgumentException("books must not be null");
        }

        final List<Book> snapshot = new ArrayList<>(books);
        final Path parent = dataPath.getParent();
        final Path directory = parent == null
                ? dataPath.toAbsolutePath().normalize().getParent()
                : parent;
        final Path effectiveDirectory = directory == null
                ? dataPath.toAbsolutePath().normalize().getParent()
                : directory;

        Path temporary = null;
        try {
            io.createDirectories(effectiveDirectory);
            temporary = io.createTempFile(effectiveDirectory,
                    "reading-shelf-", ".tmp");
            final byte[] bytes = objectMapper.writeValueAsBytes(
                    new ShelfDocument(List.copyOf(snapshot)));
            io.writeAndForce(temporary, bytes);
        } catch (IOException | RuntimeException exception) {
            cleanupTemporary(temporary);
            throw saveFailure("Unable to prepare runtime JSON", exception);
        }

        try {
            io.atomicReplace(temporary, dataPath);
            temporary = null;
        } catch (IOException | RuntimeException exception) {
            cleanupTemporary(temporary);
            throw saveFailure("Unable to atomically replace runtime JSON", exception);
        }

        try {
            io.syncDirectory(effectiveDirectory);
            return SaveResult.FULL_DURABILITY;
        } catch (UnsupportedOperationException exception) {
            return SaveResult.PLATFORM_LIMITED_DURABILITY;
        } catch (IOException | RuntimeException exception) {
            failClosed = true;
            throw new IOException("PERSISTENCE_ERROR: Parent-directory synchronization failed",
                    exception);
        }
    }

    public void saveBooks(List<Book> books) throws IOException {
        save(books);
    }

    private void cleanupTemporary(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            io.deleteIfExists(temporary);
        } catch (Exception ignored) {
            // Preserve the original persistence failure.
        }
    }

    private static IOException saveFailure(String message, Exception exception) {
        if (exception instanceof IOException ioException) {
            return new IOException(message + ": " + ioException.getMessage(), ioException);
        }
        return new IOException(message, exception);
    }

    private record ShelfDocument(List<Book> books) {
    }

    public enum SaveResult {
        FULL_DURABILITY,
        PLATFORM_LIMITED_DURABILITY
    }
}
