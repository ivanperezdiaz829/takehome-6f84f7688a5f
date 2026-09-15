package es.workfactory.occupancy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** This one passes from the very first moment: it is your harness, so you do not start from zero. */
class AgesTest {

    @Test
    void ageDoesNotDriftWithTheTimeZone() {
        assertEquals(2, Ages.on("2024-01-01", "2026-01-01"));
        assertEquals(1, Ages.on("2024-12-31", "2026-12-30"));
    }

    // From here on, it is yours.
}
