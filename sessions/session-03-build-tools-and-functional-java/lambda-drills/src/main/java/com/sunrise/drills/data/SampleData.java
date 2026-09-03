package com.sunrise.drills.data;

import com.sunrise.drills.model.Employee;

import java.util.List;

/**
 * The data every drill runs against.
 *
 * DO NOT MODIFY — the tests know these numbers by heart.
 */
public final class SampleData {

    private SampleData() {
    }

    public static List<Employee> employees() {
        return List.of(
                new Employee(1, "Alice", "Engineering", 85000, 5, "Phnom Penh"),
                new Employee(2, "Bob", "Engineering", 72000, 3, "Siem Reap"),
                new Employee(3, "Charlie", "HR", 55000, 2, "Phnom Penh"),
                new Employee(4, "Diana", "Engineering", 95000, 8, "Battambang"),
                new Employee(5, "Eve", "Marketing", 62000, 4, "Phnom Penh"),
                new Employee(6, "Frank", "HR", 58000, 6, "Siem Reap"),
                new Employee(7, "Grace", "Marketing", 67000, 3, "Battambang"),
                new Employee(8, "Henry", "Engineering", 110000, 10, "Phnom Penh"),
                new Employee(9, "Ivy", "Marketing", 48000, 1, "Siem Reap"),
                new Employee(10, "Jack", "HR", 52000, 2, "Phnom Penh"));
    }

    public static List<Integer> numbers() {
        return List.of(3, 7, 2, 8, 1, 9, 4, 6, 5, 10, 12, 15, 18, 21, 25);
    }

    public static List<String> names() {
        return List.of("Alice", "Bob", "Charlie", "Diana", "Eve",
                "Frank", "Grace", "Henry", "Ivy", "Jack");
    }
}
