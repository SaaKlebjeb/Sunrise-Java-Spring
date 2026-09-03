package com.sunrise.drills.model;

/**
 * One employee. A record, exactly as you saw in the previous session:
 * the compiler writes the accessors, {@code equals}, {@code hashCode}
 * and {@code toString} for you.
 *
 * DO NOT MODIFY.
 *
 * The accessors are {@code name()}, {@code salary()}, ... — no {@code get} prefix.
 */
public record Employee(
        int id,
        String name,
        String department,
        double salary,
        int yearsOfExperience,
        String city) {
}
