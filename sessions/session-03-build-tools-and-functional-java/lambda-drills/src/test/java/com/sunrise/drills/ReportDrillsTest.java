package com.sunrise.drills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRILL SET C — 7 checks.
 *
 * DO NOT MODIFY.
 */
@DisplayName("Set C — Streams that produce an answer")
class ReportDrillsTest {

    private ReportDrills drills;

    @BeforeEach
    void setUp() {
        drills = new ReportDrills();
    }

    @Test
    @DisplayName("C1 · Engineering headcount")
    void engineeringHeadcount() {
        assertEquals(4L, drills.engineeringHeadcount());
    }

    @Test
    @DisplayName("C2 · total payroll")
    void totalPayroll() {
        assertEquals(704000.0, drills.totalPayroll(), 0.0001);
    }

    @Test
    @DisplayName("C3 · average salary")
    void averageSalary() {
        assertEquals(70400.0, drills.averageSalary(), 0.0001);
    }

    @Test
    @DisplayName("C4 · the highest-paid employee")
    void highestPaid() {
        var result = drills.highestPaid();
        assertNotNull(result, "return an Optional, never null");
        assertTrue(result.isPresent(), "still empty — write the pipeline");
        assertEquals("Henry", result.get().name());
    }

    @Test
    @DisplayName("C5 · does everyone earn at least X")
    void everyoneEarnsAtLeast() {
        assertTrue(drills.everyoneEarnsAtLeast(48000), "Ivy earns exactly 48000");
        assertFalse(drills.everyoneEarnsAtLeast(50000), "Ivy earns less than 50000");
    }

    @Test
    @DisplayName("C6 · every name in one comma-separated String")
    void allNamesCommaSeparated() {
        var result = drills.allNamesCommaSeparated();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals("Alice, Bob, Charlie, Diana, Eve, Frank, Grace, Henry, Ivy, Jack", result);
    }

    @Test
    @DisplayName("C7 · names grouped by department")
    void namesByDepartment() {
        Map<String, List<String>> result = drills.namesByDepartment();
        assertNotNull(result, "still returning null — write the pipeline");
        assertEquals(3, result.size(), "three departments");
        assertEquals(List.of("Alice", "Bob", "Diana", "Henry"), result.get("Engineering"));
        assertEquals(List.of("Charlie", "Frank", "Jack"), result.get("HR"));
        assertEquals(List.of("Eve", "Grace", "Ivy"), result.get("Marketing"));
    }
}
