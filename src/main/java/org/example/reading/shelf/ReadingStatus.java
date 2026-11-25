package org.example.reading.shelf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The reading states supported by the personal reading shelf.
 */
public enum ReadingStatus {
    TO_READ("To Read"),
    READING("Reading"),
    FINISHED("Finished");

    private final String value;

    ReadingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static ReadingStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Reading status is required");
        }

        for (ReadingStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported reading status: " + value);
    }
}
