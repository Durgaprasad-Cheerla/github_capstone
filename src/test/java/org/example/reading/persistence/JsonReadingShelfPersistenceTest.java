package org.example.reading.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonReadingShelfPersistenceTest {
    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("FR-06, NFR-04: missing runtime file loads as an empty shelf")
    void missingFileLoadsEmpty(@TempDir Path isolatedDirectory) throws IOException {
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(isolatedDirectory.resolve("missing.json"));

        assertTrue(persistence.load().isEmpty());
        assertFalse(persistence.isFailClosed());
    }

    @Test
    @DisplayName("FR-06, NFR-02: malformed runtime JSON fails without overwriting")
    void malformedJsonFailsWithoutOverwrite() throws IOException {
        Path path = tempDirectory.resolve("malformed.json");
        Files.writeString(path, "{ definitely-not-json", StandardCharsets.UTF_8);

        JsonReadingShelfPersistence persistence = new JsonReadingShelfPersistence(path);

        assertThrows(IOException.class, persistence::load);
        assertEquals("{ definitely-not-json",
                Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("NFR-02: unknown runtime JSON fields fail explicitly")
    void unknownJsonFieldsFailWithoutOverwrite() throws IOException {
        Path path = tempDirectory.resolve("unknown-field.json");
        Files.writeString(path,
                "{\"books\":[],\"unexpected\":\"value\"}",
                StandardCharsets.UTF_8);

        JsonReadingShelfPersistence persistence = new JsonReadingShelfPersistence(path);

        assertThrows(IOException.class, persistence::load);
        assertEquals("{\"books\":[],\"unexpected\":\"value\"}",
                Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("FR-06: unreadable runtime JSON is reported explicitly")
    void unreadableJsonFailsExplicitly() throws IOException {
        Path path = tempDirectory.resolve("unreadable.json");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.exists(path)).thenReturn(true);
        when(io.readAllBytes(path)).thenThrow(new IOException("permission denied"));

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        IOException failure = assertThrows(IOException.class, persistence::load);
        assertTrue(failure.getMessage().contains("Unable to read runtime JSON"));
    }

    @Test
    @DisplayName("FR-06, NFR-02: save and load preserve inventory through durable operations")
    void saveAndLoadRoundTrip() throws IOException {
        Path path = tempDirectory.resolve("shelf.json");
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, ioWithoutDirectorySync());
        List<Book> books = List.of(
                new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ),
                new Book("The Hobbit", "J.R.R. Tolkien", ReadingStatus.FINISHED));

        persistence.save(books);
        List<Book> loaded = persistence.load();

        assertEquals(books, loaded);
        assertTrue(Files.exists(path));
    }

    @Test
    @DisplayName("NFR-02: file flush failure is reported and leaves no committed file")
    void flushFailureIsReported() throws IOException {
        Path path = tempDirectory.resolve("flush.json");
        Path parent = path.getParent();
        Path temporary = tempDirectory.resolve("temporary.tmp");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.createTempFile(eq(parent), any(), any())).thenReturn(temporary);

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        org.mockito.Mockito.doThrow(new IOException("flush failed"))
                .when(io).writeAndForce(eq(temporary), any(byte[].class));

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ))));
        assertTrue(failure.getMessage().contains("Unable to prepare runtime JSON"));
        assertFalse(Files.exists(path));
        verify(io, never()).atomicReplace(any(), any());
    }

    @Test
    @DisplayName("NFR-02: replacement failure is reported and temporary file is cleaned")
    void replacementFailureIsReportedAndCleaned() throws IOException {
        Path path = tempDirectory.resolve("replace.json");
        Path parent = path.getParent();
        Path temporary = tempDirectory.resolve("temporary.tmp");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.createTempFile(eq(parent), any(), any())).thenReturn(temporary);
        org.mockito.Mockito.doThrow(new IOException("replace failed"))
                .when(io).atomicReplace(temporary, path);

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        IOException failure = assertThrows(IOException.class,
                () -> persistence.save(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ))));
        assertTrue(failure.getMessage().contains("Unable to atomically replace runtime JSON"));
        verify(io).deleteIfExists(temporary);
    }

    @Test
    @DisplayName("NFR-02: post-replacement directory sync failure fails closed")
    void directorySyncFailureFailsClosed() throws IOException {
        Path path = tempDirectory.resolve("sync.json");
        Path parent = path.getParent();
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        Path temporary = tempDirectory.resolve("temporary.tmp");
        when(io.createTempFile(eq(parent), any(), any())).thenReturn(temporary);
        org.mockito.Mockito.doThrow(new IOException("directory sync failed"))
                .when(io).syncDirectory(parent);

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        IOException saveFailure = assertThrows(IOException.class,
                () -> persistence.save(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ))));
        assertTrue(saveFailure.getMessage().contains("PERSISTENCE_ERROR"));
        assertTrue(persistence.isFailClosed());

        IOException loadFailure = assertThrows(IOException.class, persistence::load);
        assertTrue(loadFailure.getMessage().contains("PERSISTENCE_ERROR"));
        assertThrows(IOException.class, () -> persistence.save(List.of()));
    }

    @Test
    @DisplayName("CR-01, FR-06: unsupported directory sync returns platform-limited success")
    void unsupportedDirectorySyncReturnsLimitedSuccess() throws IOException {
        Path path = tempDirectory.resolve("unsupported-sync.json");
        ReadingShelfIo io = ioWithoutDirectorySync();
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        JsonReadingShelfPersistence.SaveResult result =
                persistence.saveWithResult(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.READING)));

        assertEquals(JsonReadingShelfPersistence.SaveResult.PLATFORM_LIMITED_DURABILITY, result);
        assertFalse(persistence.isFailClosed());
    }

    @Test
    @DisplayName("CR-01: AccessDenied directory sync is classified as unsupported")
    void accessDeniedDirectorySyncReturnsLimitedSuccess() throws IOException {
        Path path = tempDirectory.resolve("access-denied-sync.json");
        Path parent = path.getParent();
        Path temporary = tempDirectory.resolve("access-denied.tmp");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.createTempFile(eq(parent), any(), any())).thenReturn(temporary);
        org.mockito.Mockito.doThrow(new UnsupportedOperationException(
                        "Parent-directory synchronization is unsupported on this platform"))
                .when(io).syncDirectory(parent);

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        JsonReadingShelfPersistence.SaveResult result =
                persistence.saveWithResult(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.READING)));

        assertEquals(JsonReadingShelfPersistence.SaveResult.PLATFORM_LIMITED_DURABILITY, result);
        assertFalse(persistence.isFailClosed());
    }

    @Test
    @DisplayName("CR-01, FR-06: supported directory sync returns full durability")
    void supportedDirectorySyncReturnsFullDurability() throws IOException {
        Path path = tempDirectory.resolve("supported-sync.json");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        Path parent = path.getParent();
        Path temporary = tempDirectory.resolve("supported.tmp");
        when(io.createTempFile(eq(parent), any(), any())).thenReturn(temporary);

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        JsonReadingShelfPersistence.SaveResult result =
                persistence.saveWithResult(List.of(
                        new Book("Dune", "Frank Herbert", ReadingStatus.READING)));

        assertEquals(JsonReadingShelfPersistence.SaveResult.FULL_DURABILITY, result);
        verify(io).syncDirectory(parent);
        assertFalse(persistence.isFailClosed());
    }

    @Test
    @DisplayName("FR-06: Jackson rejects missing books field")
    void missingBooksFieldIsMalformed() throws IOException {
        Path path = tempDirectory.resolve("missing-books.json");
        Files.writeString(path, "{}", StandardCharsets.UTF_8);
        JsonReadingShelfPersistence persistence = new JsonReadingShelfPersistence(path);

        IOException failure = assertThrows(IOException.class, persistence::load);
        assertTrue(failure.getMessage().contains("Malformed runtime JSON"));
    }

    @Test
    @DisplayName("FR-06: unreadable existence check is explicit")
    void existenceCheckFailureIsExplicit() throws IOException {
        Path path = tempDirectory.resolve("exists.json");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.exists(path)).thenThrow(new IOException("cannot stat"));

        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, io);

        IOException failure = assertThrows(IOException.class, persistence::load);
        assertTrue(failure.getMessage().contains("Unable to read runtime JSON"));
    }

    @Test
    @DisplayName("NFR-02: invalid null inventory is rejected before persistence")
    void nullInventoryIsRejected() {
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(tempDirectory.resolve("null.json"));

        assertThrows(IllegalArgumentException.class, () -> persistence.save(null));
    }

    @Test
    @DisplayName("FR-06: persisted JSON stores only title, author, and status")
    void persistedDocumentShapeIsStable() throws IOException {
        Path path = tempDirectory.resolve("shape.json");
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(path, new ObjectMapper(),
                        ioWithoutDirectorySync());

        persistence.save(List.of(
                new Book("Dune", "Frank Herbert", ReadingStatus.READING)));

        String json = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"title\""));
        assertTrue(json.contains("\"author\""));
        assertTrue(json.contains("\"status\""));
    }

    private static ReadingShelfIo ioWithoutDirectorySync() {
        ReadingShelfIo nio = ReadingShelfIo.nio();
        return new ReadingShelfIo() {
            @Override
            public boolean exists(Path path) throws IOException {
                return nio.exists(path);
            }

            @Override
            public byte[] readAllBytes(Path path) throws IOException {
                return nio.readAllBytes(path);
            }

            @Override
            public void createDirectories(Path directory) throws IOException {
                nio.createDirectories(directory);
            }

            @Override
            public Path createTempFile(Path directory, String prefix, String suffix)
                    throws IOException {
                return nio.createTempFile(directory, prefix, suffix);
            }

            @Override
            public void writeAndForce(Path file, byte[] contents) throws IOException {
                nio.writeAndForce(file, contents);
            }

            @Override
            public void atomicReplace(Path source, Path target) throws IOException {
                nio.atomicReplace(source, target);
            }

            @Override
            public void syncDirectory(Path directory) {
                throw new UnsupportedOperationException(
                        "Parent-directory sync is unsupported on this platform");
            }

            @Override
            public void deleteIfExists(Path path) throws IOException {
                nio.deleteIfExists(path);
            }
        };
    }
}
