package org.example.reading.persistence;

import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;
import org.example.reading.service.ShelfService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonReadingShelfPersistenceIntegrationTest {

    @Test
    @DisplayName("FR-06, NFR-04: temporary save and load stay isolated")
    void temporarySaveAndLoadStayIsolated(@TempDir Path isolatedDirectory) throws IOException {
        Path dataPath = isolatedDirectory.resolve("nested").resolve("reading-shelf.json");
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, ioWithoutDirectorySync());
        List<Book> inventory = List.of(
                new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ),
                new Book("The Left Hand of Darkness", "Ursula K. Le Guin",
                        ReadingStatus.READING));

        persistence.save(inventory);

        assertEquals(inventory, persistence.load());
        assertTrue(Files.isRegularFile(dataPath));
        assertTrue(Files.readString(dataPath, StandardCharsets.UTF_8).contains("\"books\""));
    }

    @Test
    @DisplayName("FR-06, NFR-02: malformed temporary JSON is explicit and not overwritten")
    void malformedTemporaryJsonIsExplicitAndNotOverwritten(@TempDir Path isolatedDirectory)
            throws IOException {
        Path dataPath = isolatedDirectory.resolve("malformed.json");
        Files.writeString(dataPath, "{ definitely-not-json", StandardCharsets.UTF_8);
        JsonReadingShelfPersistence persistence = new JsonReadingShelfPersistence(dataPath);

        IOException failure = assertThrows(IOException.class, persistence::load);

        assertTrue(failure.getMessage().contains("Malformed runtime JSON"));
        assertEquals("{ definitely-not-json",
                Files.readString(dataPath, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("NFR-02: deterministic unreadable read failure is explicit")
    void unreadableReadFailureIsExplicit(@TempDir Path isolatedDirectory) throws IOException {
        Path dataPath = isolatedDirectory.resolve("unreadable.json");
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.exists(dataPath)).thenReturn(true);
        when(io.readAllBytes(dataPath)).thenThrow(new IOException("permission denied"));
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, io);

        IOException failure = assertThrows(IOException.class, persistence::load);

        assertTrue(failure.getMessage().contains("Unable to read runtime JSON"));
        assertTrue(failure.getMessage().contains(dataPath.toString()));
    }

    @Test
    @DisplayName("NFR-02: unwritable directory failure does not commit a shelf mutation")
    void unwritableDirectoryFailureDoesNotCommitMutation(@TempDir Path isolatedDirectory)
            throws IOException {
        Path dataPath = isolatedDirectory.resolve("unwritable").resolve("reading-shelf.json");
        ReadingShelfIo io = emptyIo();
        doThrow(new IOException("directory unwritable"))
                .when(io).createDirectories(dataPath.getParent());
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, io);
        ShelfService service = new ShelfService(persistence);

        IOException failure = assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ));

        assertTrue(failure.getMessage().contains("Unable to prepare runtime JSON"));
        assertEquals(List.of(), service.listBooks());
        assertFalse(Files.exists(dataPath));
    }

    @Test
    @DisplayName("NFR-02: file flush failure is deterministic and leaves candidate uncommitted")
    void fileFlushFailureLeavesCandidateUncommitted(@TempDir Path isolatedDirectory)
            throws IOException {
        Path dataPath = isolatedDirectory.resolve("flush.json");
        ReadingShelfIo io = emptyIo();
        Path temporary = isolatedDirectory.resolve("reading-shelf.tmp");
        when(io.createTempFile(eq(dataPath.getParent()), anyString(), anyString()))
                .thenReturn(temporary);
        doThrow(new IOException("flush failed"))
                .when(io).writeAndForce(eq(temporary), any(byte[].class));
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, io);
        ShelfService service = new ShelfService(persistence);

        IOException failure = assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ));

        assertTrue(failure.getMessage().contains("Unable to prepare runtime JSON"));
        assertEquals(List.of(), service.listBooks());
        verify(io, never()).atomicReplace(any(), any());
        assertFalse(Files.exists(dataPath));
    }

    @Test
    @DisplayName("NFR-02: atomic replacement failure cleans temporary and does not commit")
    void atomicReplacementFailureDoesNotCommit(@TempDir Path isolatedDirectory)
            throws IOException {
        Path dataPath = isolatedDirectory.resolve("replace.json");
        ReadingShelfIo io = emptyIo();
        Path temporary = isolatedDirectory.resolve("replace.tmp");
        when(io.createTempFile(eq(dataPath.getParent()), anyString(), anyString()))
                .thenReturn(temporary);
        doThrow(new IOException("replace failed"))
                .when(io).atomicReplace(temporary, dataPath);
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, io);
        ShelfService service = new ShelfService(persistence);

        IOException failure = assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ));

        assertTrue(failure.getMessage().contains("Unable to atomically replace runtime JSON"));
        assertEquals(List.of(), service.listBooks());
        verify(io).deleteIfExists(temporary);
        assertFalse(Files.exists(dataPath));
    }

    @Test
    @DisplayName("F-02, FR-06, NFR-02: post-replacement sync failure fails closed")
    void postReplacementSyncFailureFailsClosed(@TempDir Path isolatedDirectory)
            throws IOException {
        Path dataPath = isolatedDirectory.resolve("sync.json");
        ReadingShelfIo io = emptyIo();
        Path temporary = isolatedDirectory.resolve("sync.tmp");
        when(io.createTempFile(eq(dataPath.getParent()), anyString(), anyString()))
                .thenReturn(temporary);
        doThrow(new IOException("directory sync failed"))
                .when(io).syncDirectory(dataPath.getParent());
        JsonReadingShelfPersistence persistence =
                new JsonReadingShelfPersistence(dataPath, io);
        ShelfService service = new ShelfService(persistence);

        IOException saveFailure = assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ));

        assertTrue(saveFailure.getMessage().contains("PERSISTENCE_ERROR"));
        assertTrue(persistence.isFailClosed());
        IOException readFailure = assertThrows(IOException.class, service::listBooks);
        assertTrue(readFailure.getMessage().contains("PERSISTENCE_ERROR"));
        assertThrows(IOException.class,
                () -> service.createBook("The Hobbit", "J.R.R. Tolkien",
                        ReadingStatus.TO_READ));
    }

    private static ReadingShelfIo emptyIo() throws IOException {
        ReadingShelfIo io = mock(ReadingShelfIo.class);
        when(io.exists(any(Path.class))).thenReturn(false);
        return io;
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
                // Windows cannot open directories through FileChannel in this environment.
                // This test injects successful completion only for JSON round-trip behavior.
            }

            @Override
            public void deleteIfExists(Path path) throws IOException {
                nio.deleteIfExists(path);
            }
        };
    }
}
