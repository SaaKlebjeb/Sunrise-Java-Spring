package com.sunrise.drills;

import com.sunrise.drills.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DRILL SET B — 7 checks.
 *
 * DO NOT MODIFY.
 */
@DisplayName("Set B — Streams that reshape a list")
class StreamDrillsTest {

    private StreamDrills drills;

    @BeforeEach
    void setUp() {
        drills = new StreamDrills();
    }

    @Test
    @DisplayName("B1 · the even numbers, original order")
    void evenNumbers() {
        var result = drills.evenNumbers();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of(2, 8, 4, 6, 10, 12, 18), result);
    }

    @Test
    @DisplayName("B2 · every name upper-cased")
    void shoutedNames() {
        var result = drills.shoutedNames();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("ALICE", "BOB", "CHARLIE", "DIANA", "EVE",
                "FRANK", "GRACE", "HENRY", "IVY", "JACK"), result);
    }

    @Test
    @DisplayName("B3 · the name of every employee")
    void employeeNames() {
        var result = drills.employeeNames();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("Alice", "Bob", "Charlie", "Diana", "Eve",
                "Frank", "Grace", "Henry", "Ivy", "Jack"), result);
    }

    @Test
    @DisplayName("B4 · everyone over 70000")
    void highEarners() {
        var result = drills.highEarners();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("Alice", "Bob", "Diana", "Henry"),
                result.stream().map(Employee::name).toList());
    }

    @Test
    @DisplayName("B5 · the numbers, smallest first")
    void sortedNumbers() {
        var result = drills.sortedNumbers();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 18, 21, 25), result);
    }

    @Test
    @DisplayName("B6 · the three highest-paid names, highest first")
    void topThreeEarnerNames() {
        var result = drills.topThreeEarnerNames();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("Henry", "Diana", "Alice"), result);
    }

    @Test
    @DisplayName("B7 · the distinct cities, A to Z")
    void distinctCitiesSorted() {
        var result = drills.distinctCitiesSorted();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(List.of("Battambang", "Phnom Penh", "Siem Reap"), result);
    }
}
