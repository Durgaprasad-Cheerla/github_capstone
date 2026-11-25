package org.example.reading.shelf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {
    @Test
    @DisplayName("FR-01, FR-02: book preserves title, author, and initial status")
    void createsBookWithAllValues() {
        Book book = new Book("  The Hobbit  ", " J.R.R. Tolkien ", ReadingStatus.TO_READ);

        assertEquals("  The Hobbit  ", book.title());
        assertEquals(" J.R.R. Tolkien ", book.author());
        assertEquals(ReadingStatus.TO_READ, book.status());
    }

    @Test
    @DisplayName("FR-01: blank title and author are rejected")
    void rejectsBlankTitleAndAuthor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book("   ", "Tolkien", ReadingStatus.TO_READ));
        assertThrows(IllegalArgumentException.class,
                () -> new Book("The Hobbit", "\t", ReadingStatus.TO_READ));
    }

    @Test
    @DisplayName("FR-01: title and author validation distinguishes invalid fields")
    void exposesWhitespaceValidation() {
        assertFalse(Book.isValidTitle(" "));
        assertFalse(Book.isValidAuthor("\n"));
        assertTrue(Book.isValidTitle("Dune"));
        assertTrue(Book.isValidAuthor("Frank Herbert"));
    }

    @Test
    @DisplayName("FR-03, FR-04: status can change without changing title-author identity")
    void changesStatusWithIdentityPreserved() {
        Book original = new Book("Dune", "Frank Herbert", ReadingStatus.TO_READ);

        Book changed = original.withStatus(ReadingStatus.FINISHED);

        assertEquals(original.title(), changed.title());
        assertEquals(original.author(), changed.author());
        assertEquals(ReadingStatus.FINISHED, changed.status());
    }

    @Test
    @DisplayName("FR-01: null status is rejected")
    void rejectsNullStatus() {
        assertThrows(NullPointerException.class,
                () -> new Book("Dune", "Frank Herbert", null));
    }
}
