package org.example.reading.shelf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadingStatusTest {
    @Test
    @DisplayName("FR-01, FR-03: all supported status values and display names are available")
    void exposesSupportedStatuses() {
        assertEquals("To Read", ReadingStatus.TO_READ.getValue());
        assertEquals("Reading", ReadingStatus.READING.getValue());
        assertEquals("Finished", ReadingStatus.FINISHED.getValue());
    }

    @Test
    @DisplayName("FR-01: status parsing rejects unsupported values")
    void rejectsUnsupportedStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> ReadingStatus.fromValue("Paused"));
    }

    @Test
    @DisplayName("FR-03: all six ordered distinct status transitions are representable")
    void representsDirectTransitions() {
        ReadingStatus[] statuses = ReadingStatus.values();
        int transitionCount = 0;
        for (ReadingStatus from : statuses) {
            for (ReadingStatus to : statuses) {
                if (from != to) {
                    transitionCount++;
                }
            }
        }
        assertEquals(6, transitionCount);
    }

    @Test
    @DisplayName("FR-01: status parsing accepts exact API labels")
    void parsesSupportedStatusNames() {
        assertEquals(ReadingStatus.TO_READ, ReadingStatus.fromValue("To Read"));
        assertEquals(ReadingStatus.READING, ReadingStatus.fromValue("Reading"));
        assertEquals(ReadingStatus.FINISHED, ReadingStatus.fromValue("Finished"));
    }

    @Test
    @DisplayName("FR-01: status parsing rejects case, whitespace, and enum-name variants")
    void rejectsVariantStatusNames() {
        assertThrows(IllegalArgumentException.class, () -> ReadingStatus.fromValue("READING"));
        assertThrows(IllegalArgumentException.class, () -> ReadingStatus.fromValue("finished"));
        assertThrows(IllegalArgumentException.class, () -> ReadingStatus.fromValue(" Reading "));
    }
}
