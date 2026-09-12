package com.sunrise.drills;

import com.sunrise.drills.data.SampleData;
import com.sunrise.drills.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRILL SET A — 8 checks.
 *
 * You do not have to understand this file yet. You will write files
 * like it in the next session. Today you only run it.
 *
 * DO NOT MODIFY.
 */
@DisplayName("Set A — Lambdas")
class LambdaDrillsTest {

    private LambdaDrills drills;

    @BeforeEach
    void setUp() {
        drills = new LambdaDrills();
    }

    @Test
    @DisplayName("A1 · a Predicate that is true for even numbers")
    void isEven() {
        var isEven = drills.isEven();
        assertNotNull(isEven, "still returning null — write the lambda");
        assertTrue(isEven.test(4));
        assertTrue(isEven.test(0));
        assertFalse(isEven.test(1));
    }

    @Test
    @DisplayName("A2 · a Function that shouts")
    void shout() {
        var shout = drills.shout();
        assertNotNull(shout, "still returning null — write the lambda");
        assertEquals("siem reap!", shout.apply("SIEM REAP"));
    }

    @Test
    @DisplayName("A3 · a Consumer that appends to a StringBuilder")
    void markDone() {
        var markDone = drills.markDone();
        assertNotNull(markDone, "still returning null — write the lambda");
        var sb = new StringBuilder("task");
        markDone.accept(sb);
        assertEquals("task done", sb.toString());
    }

    @Test
    @DisplayName("A4 · a Supplier that produces a NEW list every call")
    void newList() {
        var newList = drills.newList();
        assertNotNull(newList, "still returning null — write the lambda");
        List<String> first = newList.get();
        List<String> second = newList.get();
        assertNotNull(first);
        assertTrue(first.isEmpty(), "the list should start empty");
        assertNotSame(first, second,
                "get() handed back the same list twice — it must build a new one each call");
        first.add("x");
        assertTrue(second.isEmpty(), "adding to one list changed the other");
    }

    @Test
    @DisplayName("A5 · a Comparator, highest salary first")
    void bySalaryHighestFirst() {
        var comparator = drills.bySalaryHighestFirst();
        assertNotNull(comparator, "still returning null — write the lambda");
        var sorted = new ArrayList<>(SampleData.employees());
        sorted.sort(comparator);
        assertEquals("Henry", sorted.get(0).name(), "Henry earns the most");
        assertEquals("Ivy", sorted.get(sorted.size() - 1).name(), "Ivy earns the least");
    }

    @Test
    @DisplayName("A6 · your own functional interface")
    void tenPercentOff() {
        var discount = drills.tenPercentOff();
        assertNotNull(discount, "still returning null — write the lambda");
        assertEquals(180.0, discount.applyTo(200.0), 0.0001);
        assertEquals(0.0, discount.applyTo(0.0), 0.0001);
    }

    @Test
    @DisplayName("A7 · a lambda that captures a parameter")
    void greeter() {
        var greeter = drills.greeter("Sokha");
        assertNotNull(greeter, "still returning null — write the lambda");
        assertEquals("Hello, Sokha", greeter.get());
        assertEquals("Hello, Dara", drills.greeter("Dara").get());
    }

    @Test
    @DisplayName("A8 · two Predicates combined with .and(...)")
    void seniorEngineer() {
        var seniorEngineer = drills.seniorEngineer();
        assertNotNull(seniorEngineer, "still returning null — write the lambda");
        List<String> matched = SampleData.employees().stream()
                .filter(seniorEngineer)
                .map(Employee::name)
                .toList();
        assertEquals(List.of("Alice", "Diana", "Henry"), matched);
    }
}
