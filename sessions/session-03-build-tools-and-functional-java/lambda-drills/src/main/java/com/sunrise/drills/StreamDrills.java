package com.sunrise.drills;

import com.sunrise.drills.data.SampleData;
import com.sunrise.drills.model.Employee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * DRILL SET B — Streams that reshape a list
 * ============================================================
 * Same rule as set A: the loop above each method already works.
 * Replace it with a stream pipeline that produces exactly the
 * same list.
 *
 * filter → keeps elements
 * map    → transforms elements
 * sorted → orders them
 * distinct / limit → thin them out
 * .toList() → hands you the finished list
 *
 * Run StreamDrillsTest to check your answers.
 * ============================================================
 */
public class StreamDrills {

    private final List<Employee> employees = SampleData.employees();
    private final List<Integer> numbers = SampleData.numbers();
    private final List<String> names = SampleData.names();

    // --------------------------------------------------------
    // B1 — keep the even numbers, in their original order
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<Integer> result = new ArrayList<>();
    //   for (Integer n : numbers) {
    //       if (n % 2 == 0) {
    //           result.add(n);
    //       }
    //   }
    //   return result;
    // --------------------------------------------------------
    public List<Integer> evenNumbers() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B2 — every name in upper case
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<String> result = new ArrayList<>();
    //   for (String name : names) {
    //       result.add(name.toUpperCase());
    //   }
    //   return result;
    //
    // A method reference reads better than a lambda here.
    // --------------------------------------------------------
    public List<String> shoutedNames() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B3 — the name of every employee
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<String> result = new ArrayList<>();
    //   for (Employee e : employees) {
    //       result.add(e.name());
    //   }
    //   return result;
    // --------------------------------------------------------
    public List<String> employeeNames() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B4 — everyone earning strictly more than 70000
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<Employee> result = new ArrayList<>();
    //   for (Employee e : employees) {
    //       if (e.salary() > 70000) {
    //           result.add(e);
    //       }
    //   }
    //   return result;
    // --------------------------------------------------------
    public List<Employee> highEarners() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B5 — the numbers, smallest first
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<Integer> result = new ArrayList<>(numbers);
    //   Collections.sort(result);
    //   return result;
    //
    // Note what the loop had to do first: copy the list, because
    // SampleData hands you an immutable one. A stream never
    // touches its source, so you do not need the copy.
    // --------------------------------------------------------
    public List<Integer> sortedNumbers() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B6 — the names of the three highest-paid employees,
    //      highest first
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<Employee> copy = new ArrayList<>(employees);
    //   copy.sort(new Comparator<Employee>() {
    //       @Override
    //       public int compare(Employee a, Employee b) {
    //           return Double.compare(b.salary(), a.salary());
    //       }
    //   });
    //   List<String> result = new ArrayList<>();
    //   for (int i = 0; i < 3 && i < copy.size(); i++) {
    //       result.add(copy.get(i).name());
    //   }
    //   return result;
    //
    // Three operations in one pipeline: sorted → limit → map.
    // Expected: [Henry, Diana, Alice]
    // --------------------------------------------------------
    public List<String> topThreeEarnerNames() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // B7 — every city that appears, with no repeats, A to Z
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   List<String> result = new ArrayList<>();
    //   for (Employee e : employees) {
    //       if (!result.contains(e.city())) {
    //           result.add(e.city());
    //       }
    //   }
    //   Collections.sort(result);
    //   return result;
    //
    // Expected: [Battambang, Phnom Penh, Siem Reap]
    // --------------------------------------------------------
    public List<String> distinctCitiesSorted() {
        // TODO: rewrite as a stream
        return null;
    }
}
