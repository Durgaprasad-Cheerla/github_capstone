package org.example.reading.service;

import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {
    @Mock
    JsonReadingShelfPersistence persistence;

    ShelfService service;

    @BeforeEach
    void setUp() throws IOException {
        when(persistence.load()).thenReturn(new ArrayList<>());
        service = new ShelfService(persistence);
    }

    @Test
    @DisplayName("FR-01, FR-05: create reports separate title and author field errors")
    void createReportsSeparateFieldErrors() {
        ShelfService.ValidationException titleAndAuthorError =
                assertThrows(ShelfService.ValidationException.class,
                        () -> service.createBook(" ", "", ReadingStatus.TO_READ));

        Map<String, String> errors = titleAndAuthorError.getFieldErrors();
        assertTrue(errors.containsKey("title"));
        assertTrue(errors.containsKey("author"));
    }

    @Test
    @DisplayName("FR-01, FR-05: create rejects missing and unsupported status")
    void createRejectsMissingAndUnsupportedStatus() {
        ShelfService.ValidationException missingStatus =
                assertThrows(ShelfService.ValidationException.class,
                        () -> service.createBook("Dune", "Frank Herbert", (ReadingStatus) null));
        assertTrue(missingStatus.getFieldErrors().containsKey("status"));

        ShelfService.ValidationException unsupported =
                assertThrows(ShelfService.ValidationException.class,
                        () -> service.createBook("Dune", "Frank Herbert", "Finished "));
        assertTrue(unsupported.getFieldErrors().containsKey("status"));
    }

    @Test
    @DisplayName("FR-01, FR-02, FR-06: create saves and commits the resulting snapshot")
    void createSavesAndCommits() throws IOException {
        Book created = service.createBook(" Dune ", " Frank Herbert ",
                ReadingStatus.TO_READ);

        assertEquals(List.of(created), service.listBooks());
        assertEquals("Dune", created.title());
        assertEquals("Frank Herbert", created.author());
        assertEquals(ReadingStatus.TO_READ, created.status());
        verify(persistence).saveWithResult(List.of(created));
    }

    @Test
    @DisplayName("FR-04: duplicate matching is trimmed, case-insensitive and ignores status")
    void duplicateMatchingIsTrimmedCaseInsensitiveAndIgnoresStatus() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);
        assertThrows(ShelfService.DuplicateBookException.class,
                () -> service.createBook("Dune", "Frank Herbert", ReadingStatus.FINISHED));

        assertEquals(1, service.listBooks().size());
        assertEquals(ReadingStatus.TO_READ, service.listBooks().get(0).status());
    }

    @Test
    @DisplayName("FR-04: create stores trimmed values and keeps first-entered casing and inner spacing")
    void createStoresTrimmedValuesWithFirstEnteredCasing() throws IOException {
        Book created = service.createBook("  dUNE  ", "	Frank  HERBERT ",
                ReadingStatus.TO_READ);

        assertEquals("dUNE", created.title());
        assertEquals("Frank  HERBERT", created.author());
        assertEquals(List.of(created), service.listBooks());
    }

    @Test
    @DisplayName("FR-04: duplicate matching ignores case, surrounding whitespace and status")
    void duplicateMatchingIgnoresCaseAndSurroundingWhitespace() throws IOException {
        Book first = service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        ShelfService.DuplicateBookException duplicate =
                assertThrows(ShelfService.DuplicateBookException.class,
                        () -> service.createBook(" dUNE ", "  FRANK herbert	",
                                ReadingStatus.FINISHED));

        assertEquals("dUNE", duplicate.getTitle());
        assertEquals(List.of(first), service.listBooks());
        verify(persistence, times(1)).saveWithResult(anyList());
    }

    @Test
    @DisplayName("FR-04: inner whitespace is not collapsed for identity matching")
    void innerWhitespaceIsNotCollapsed() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        service.createBook("Dune", "Frank  Herbert", ReadingStatus.TO_READ);

        assertEquals(2, service.listBooks().size());
    }

    @Test
    @DisplayName("FR-03: status change finds a book ignoring case and surrounding whitespace")
    void statusChangeLookupIgnoresCaseAndSurroundingWhitespace() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        Book changed = service.changeStatus(" DUNE ", " frank herbert",
                ReadingStatus.FINISHED);

        assertEquals(new Book("Dune", "Frank Herbert", ReadingStatus.FINISHED), changed);
        assertEquals(List.of(changed), service.listBooks());
    }

    @Test
    @DisplayName("FR-03: status change still reports an unknown identity after normalization")
    void statusChangeUnknownIdentityAfterNormalization() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        assertThrows(ShelfService.BookNotFoundException.class,
                () -> service.changeStatus(" Dune ", "Someone Else",
                        ReadingStatus.FINISHED));
    }

    @Test
    @DisplayName("FR-03, FR-04: previously saved untrimmed or mixed-case books are matched without migration")
    void legacyPersistedBooksAreNormalizedAtCompareTimeOnly() throws IOException {
        Book legacy = new Book("  DUNE ", " frank herbert", ReadingStatus.TO_READ);
        JsonReadingShelfPersistence legacyPersistence =
                mock(JsonReadingShelfPersistence.class);
        when(legacyPersistence.load()).thenReturn(new ArrayList<>(List.of(legacy)));
        ShelfService legacyService = new ShelfService(legacyPersistence);

        assertThrows(ShelfService.DuplicateBookException.class,
                () -> legacyService.createBook("Dune", "Frank Herbert",
                        ReadingStatus.TO_READ));
        verify(legacyPersistence, never()).saveWithResult(anyList());
        assertEquals(List.of(legacy), legacyService.listBooks());

        Book changed = legacyService.changeStatus("dune", "FRANK HERBERT",
                ReadingStatus.FINISHED);

        assertEquals(new Book("  DUNE ", " frank herbert", ReadingStatus.FINISHED), changed);
        assertEquals(List.of(changed), legacyService.listBooks());
    }

    @Test
    @DisplayName("FR-03: the service adds no length cap so older long books stay updatable")
    void serviceDoesNotCapLengths() throws IOException {
        String longTitle = "T".repeat(150);
        String longAuthor = "A".repeat(80);
        Book legacy = new Book(longTitle, longAuthor, ReadingStatus.TO_READ);
        JsonReadingShelfPersistence legacyPersistence =
                mock(JsonReadingShelfPersistence.class);
        when(legacyPersistence.load()).thenReturn(new ArrayList<>(List.of(legacy)));
        ShelfService legacyService = new ShelfService(legacyPersistence);

        Book changed = legacyService.changeStatus(longTitle, longAuthor,
                ReadingStatus.READING);

        assertEquals(ReadingStatus.READING, changed.status());
    }

    @Test
    @DisplayName("FR-03: all distinct status transitions and reversals work")
    void allStatusTransitionsAndReversalsWork() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        for (ReadingStatus first : ReadingStatus.values()) {
            for (ReadingStatus second : ReadingStatus.values()) {
                if (first != second) {
                    service.changeStatus("Dune", "Frank Herbert", second);
                    assertEquals(second,
                            service.listBooks().get(0).status());
                    service.changeStatus("Dune", "Frank Herbert", first);
                    assertEquals(first,
                            service.listBooks().get(0).status());
                }
            }
        }
    }

    @Test
    @DisplayName("FR-03: same-status change is an idempotent no-op")
    void sameStatusChangeDoesNotPersist() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        Book unchanged = service.changeStatus(
                "Dune", "Frank Herbert", ReadingStatus.TO_READ);

        assertEquals(ReadingStatus.TO_READ, unchanged.status());
        verify(persistence, times(1)).saveWithResult(anyList());
    }

    @Test
    @DisplayName("FR-03, FR-05: unknown status-change identity is reported")
    void unknownIdentityIsReported() {
        assertThrows(ShelfService.BookNotFoundException.class,
                () -> service.changeStatus("Missing", "Nobody",
                        ReadingStatus.FINISHED));
    }

    @Test
    @DisplayName("NFR-02, FR-06: failed persistence never commits a create candidate")
    void failedSaveNeverCommitsCreate() throws IOException {
        doThrow(new IOException("disk full"))
                .when(persistence).saveWithResult(anyList());

        assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert",
                        ReadingStatus.TO_READ));
        assertTrue(service.listBooks().isEmpty());
    }

    @Test
    @DisplayName("FR-03, NFR-02, FR-06: failed persistence never commits a status candidate")
    void failedSaveNeverCommitsStatusChange() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);
        doThrow(new IOException("disk full"))
                .when(persistence).saveWithResult(anyList());

        assertThrows(IOException.class,
                () -> service.changeStatus("Dune", "Frank Herbert",
                        ReadingStatus.FINISHED));
        assertEquals(ReadingStatus.TO_READ, service.listBooks().get(0).status());
    }

    @Test
    @DisplayName("FR-02: reads return immutable snapshots")
    void readsReturnImmutableSnapshots() throws IOException {
        service.createBook("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        List<Book> snapshot = service.listBooks();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new Book("Other", "Author",
                        ReadingStatus.TO_READ)));
    }

    @Test
    @DisplayName("NFR-02: post-replacement fail-closed persistence refuses reads")
    void failClosedPersistenceRefusesReads() throws IOException {
        when(persistence.isFailClosed()).thenReturn(true);

        assertThrows(IOException.class, service::listBooks);
        assertThrows(IOException.class,
                () -> service.createBook("Dune", "Frank Herbert",
                        ReadingStatus.TO_READ));
    }

    @Test
    @DisplayName("FR-01, FR-03, FR-06: concurrent mutations do not lose updates")
    void concurrentMutationsDoNotLoseUpdates() throws Exception {
        doAnswer(invocation -> JsonReadingShelfPersistence.SaveResult.FULL_DURABILITY)
                .when(persistence).saveWithResult(anyList());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> {
                await(start);
                return createForConcurrency("First", "Author");
            });
            Future<?> second = executor.submit(() -> {
                await(start);
                return createForConcurrency("Second", "Author");
            });
            start.countDown();
            first.get();
            second.get();

            assertEquals(2, service.listBooks().size());
        } finally {
            executor.shutdownNow();
        }
    }

    private Book createForConcurrency(String title, String author) {
        try {
            return service.createBook(title, author, ReadingStatus.TO_READ);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}
