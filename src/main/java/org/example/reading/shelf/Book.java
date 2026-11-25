package org.example.reading.shelf;

import java.util.Objects;

/**
 * An immutable entry in the personal reading shelf.
 */
public record Book(String title, String author, ReadingStatus status) {
    public Book {
        validateTitle(title);
        validateAuthor(author);
        Objects.requireNonNull(status, "status must not be null");
    }

    public static void validateTitle(String title) {
        if (!isValidTitle(title)) {
            throw new IllegalArgumentException("Title must not be blank");
        }
    }

    public static void validateAuthor(String author) {
        if (!isValidAuthor(author)) {
            throw new IllegalArgumentException("Author must not be blank");
        }
    }

    public static boolean isValidTitle(String title) {
        return title != null && !title.trim().isEmpty();
    }

    public static boolean isValidAuthor(String author) {
        return author != null && !author.trim().isEmpty();
    }

    public boolean hasValidTitle() {
        return isValidTitle(title);
    }

    public boolean hasValidAuthor() {
        return isValidAuthor(author);
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public Book withStatus(ReadingStatus newStatus) {
        return new Book(title, author, Objects.requireNonNull(newStatus, "status must not be null"));
    }
}
