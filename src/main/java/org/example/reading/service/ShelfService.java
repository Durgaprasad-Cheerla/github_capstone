package org.example.reading.service;

import org.example.reading.persistence.JsonReadingShelfPersistence;
import org.example.reading.shelf.Book;
import org.example.reading.shelf.ReadingStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Domain owner for the personal reading shelf.
 *
 * <p>Creates and status changes are serialized, and every inventory read
 * obtains a snapshot under the same lock.</p>
 */
public final class ShelfService {
    private final JsonReadingShelfPersistence persistence;
    private final ReentrantLock mutationLock = new ReentrantLock();
    private List<Book> inventory;
    private String lastDurabilityWarning;

    public ShelfService(JsonReadingShelfPersistence persistence) throws IOException {
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        mutationLock.lock();
        try {
            inventory = List.copyOf(persistence.load());
        } finally {
            mutationLock.unlock();
        }
    }

    public List<Book> listBooks() throws IOException {
        mutationLock.lock();
        try {
            ensureReadable();
            return List.copyOf(inventory);
        } finally {
            mutationLock.unlock();
        }
    }

    public List<Book> getInventory() throws IOException {
        return listBooks();
    }

    public List<Book> getBooks() throws IOException {
        return listBooks();
    }

    public String getLastDurabilityWarning() {
        mutationLock.lock();
        try {
            return lastDurabilityWarning;
        } finally {
            mutationLock.unlock();
        }
    }

    public Book createBook(String title, String author, ReadingStatus status)
            throws IOException {
        mutationLock.lock();
        try {
            ensureWritable();

            Map<String, String> fieldErrors = new LinkedHashMap<>();
            if (!Book.isValidTitle(title)) {
                fieldErrors.put("title", "Title must not be blank");
            }
            if (!Book.isValidAuthor(author)) {
                fieldErrors.put("author", "Author must not be blank");
            }
            if (status == null) {
                fieldErrors.put("status", "Reading status is required");
            }
            if (!fieldErrors.isEmpty()) {
                throw new ValidationException("Invalid book fields", fieldErrors);
            }

            String storedTitle = normalizeForStorage(title);
            String storedAuthor = normalizeForStorage(author);
            String titleKey = normalizeForIdentity(title);
            String authorKey = normalizeForIdentity(author);
            for (Book existing : inventory) {
                if (normalizeForIdentity(existing.title()).equals(titleKey)
                        && normalizeForIdentity(existing.author()).equals(authorKey)) {
                    throw new DuplicateBookException(
                            "A book with this title and author already exists",
                            storedTitle,
                            storedAuthor);
                }
            }

            Book created = new Book(storedTitle, storedAuthor, status);
            List<Book> candidate = new ArrayList<>(inventory);
            candidate.add(created);
            List<Book> candidateSnapshot = List.copyOf(candidate);

            JsonReadingShelfPersistence.SaveResult saveResult =
                    persistence.saveWithResult(candidateSnapshot);
            lastDurabilityWarning =
                    saveResult == JsonReadingShelfPersistence.SaveResult.PLATFORM_LIMITED_DURABILITY
                            ? "PLATFORM_LIMITED_DURABILITY"
                            : null;
            inventory = candidateSnapshot;
            return created;
        } finally {
            mutationLock.unlock();
        }
    }

    public Book createBook(String title, String author, String status)
            throws IOException {
        return createBook(title, author, parseStatus(status));
    }

    public Book changeStatus(String title, String author, ReadingStatus newStatus)
            throws IOException {
        mutationLock.lock();
        try {
            ensureWritable();

            Map<String, String> fieldErrors = new LinkedHashMap<>();
            if (!Book.isValidTitle(title)) {
                fieldErrors.put("title", "Title must not be blank");
            }
            if (!Book.isValidAuthor(author)) {
                fieldErrors.put("author", "Author must not be blank");
            }
            if (newStatus == null) {
                fieldErrors.put("status", "Reading status is required");
            }
            if (!fieldErrors.isEmpty()) {
                throw new ValidationException("Invalid book fields", fieldErrors);
            }

            String titleKey = normalizeForIdentity(title);
            String authorKey = normalizeForIdentity(author);
            int matchIndex = -1;
            for (int index = 0; index < inventory.size(); index++) {
                Book existing = inventory.get(index);
                if (normalizeForIdentity(existing.title()).equals(titleKey)
                        && normalizeForIdentity(existing.author()).equals(authorKey)) {
                    matchIndex = index;
                    break;
                }
            }
            if (matchIndex < 0) {
                throw new BookNotFoundException(
                        "No book matches the supplied title and author",
                        title,
                        author);
            }

            Book existing = inventory.get(matchIndex);
            if (existing.status() == newStatus) {
                return existing;
            }
            Book changed = existing.withStatus(newStatus);
            List<Book> candidate = new ArrayList<>(inventory);
            candidate.set(matchIndex, changed);
            List<Book> candidateSnapshot = List.copyOf(candidate);

            JsonReadingShelfPersistence.SaveResult saveResult =
                    persistence.saveWithResult(candidateSnapshot);
            lastDurabilityWarning =
                    saveResult == JsonReadingShelfPersistence.SaveResult.PLATFORM_LIMITED_DURABILITY
                            ? "PLATFORM_LIMITED_DURABILITY"
                            : null;
            inventory = candidateSnapshot;
            return changed;
        } finally {
            mutationLock.unlock();
        }
    }

    public Book changeStatus(String title, String author, String status)
            throws IOException {
        return changeStatus(title, author, parseStatus(status));
    }

    /** Stored form: trimmed only; first-entered casing and inner whitespace are kept. */
    private static String normalizeForStorage(String value) {
        return value.trim();
    }

    /** Identity form: trimmed and lower-cased; applied to both sides at compare time. */
    private static String normalizeForIdentity(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ReadingStatus parseStatus(String status) {
        try {
            return ReadingStatus.fromValue(status);
        } catch (IllegalArgumentException exception) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            fieldErrors.put("status", "Unsupported reading status");
            throw new ValidationException("Invalid book fields", fieldErrors);
        }
    }

    private void ensureReadable() throws IOException {
        if (persistence.isFailClosed()) {
            throw new IOException("PERSISTENCE_ERROR: Persistence is fail-closed");
        }
    }

    private void ensureWritable() throws IOException {
        ensureReadable();
    }

    public static final class ValidationException extends IllegalArgumentException {
        private final Map<String, String> fieldErrors;

        public ValidationException(String message, Map<String, String> fieldErrors) {
            super(message);
            this.fieldErrors = Map.copyOf(fieldErrors);
        }

        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }
    }

    public static final class DuplicateBookException extends IllegalArgumentException {
        private final String title;
        private final String author;

        public DuplicateBookException(String message, String title, String author) {
            super(message);
            this.title = title;
            this.author = author;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }
    }

    public static final class BookNotFoundException extends IllegalArgumentException {
        private final String title;
        private final String author;

        public BookNotFoundException(String message, String title, String author) {
            super(message);
            this.title = title;
            this.author = author;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }
    }
}
