package com.sunrise.drills;

import com.sunrise.drills.data.SampleData;
import com.sunrise.drills.model.Employee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ============================================================
 * DRILL SET C — Streams that produce an answer
 * ============================================================
 * Sets A and B ended with a list. These end with a number, a
 * boolean, a String or a Map — the shapes a service method
 * actually returns.
 *
 * count / sum / average / max → one value out of many
 * anyMatch / allMatch         → a yes or no, and it stops early
 * joining                     → one String
 * groupingBy                  → a Map
 *
 * Run ReportDrillsTest to check your answers.
 * ============================================================
 */
public class ReportDrills {

    private final List<Employee> employees = SampleData.employees();

    // --------------------------------------------------------
    // C1 — how many people work in Engineering
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   long count = 0;
    //   for (Employee e : employees) {
    //       if (e.department().equals("Engineering")) {
    //           count++;
    //       }
    //   }
    //   return count;
    // --------------------------------------------------------
    public long engineeringHeadcount() {
        // TODO: rewrite as a stream
        return 0;
    }

    // --------------------------------------------------------
    // C2 — the total of every salary
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   double total = 0;
    //   for (Employee e : employees) {
    //       total += e.salary();
    //   }
    //   return total;
    //
    // mapToDouble(...) turns the stream into a DoubleStream,
    // which is the one that has sum().
    // --------------------------------------------------------
    public double totalPayroll() {
        // TODO: rewrite as a stream
        return 0;
    }

    // --------------------------------------------------------
    // C3 — the average salary, or 0 if there is nobody at all
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   if (employees.isEmpty()) return 0;
    //   double total = 0;
    //   for (Employee e : employees) {
    //       total += e.salary();
    //   }
    //   return total / employees.size();
    //
    // average() hands back an OptionalDouble, precisely because
    // the average of nothing is not a number. .orElse(0) is where
    // the loop's first line went.
    // --------------------------------------------------------
    public double averageSalary() {
        // TODO: rewrite as a stream
        return 0;
    }

    // --------------------------------------------------------
    // C4 — the highest-paid employee
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Employee best = null;
    //   for (Employee e : employees) {
    //       if (best == null || e.salary() > best.salary()) {
    //           best = e;
    //       }
    //   }
    //   return Optional.ofNullable(best);
    //
    // max(...) takes a Comparator and returns an Optional, so the
    // null the loop had to invent never exists.
    // --------------------------------------------------------
    public Optional<Employee> highestPaid() {
        // TODO: rewrite as a stream
        return Optional.empty();
    }

    // --------------------------------------------------------
    // C5 — does everybody earn at least this much?
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   for (Employee e : employees) {
    //       if (e.salary() < floor) {
    //           return false;
    //       }
    //   }
    //   return true;
    //
    // allMatch stops at the first failure, exactly like the
    // early return does.
    // --------------------------------------------------------
    public boolean everyoneEarnsAtLeast(double floor) {
        // TODO: rewrite as a stream
        return false;
    }

    // --------------------------------------------------------
    // C6 — every name in one String, comma-separated
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   StringBuilder sb = new StringBuilder();
    //   for (Employee e : employees) {
    //       if (sb.length() > 0) sb.append(", ");
    //       sb.append(e.name());
    //   }
    //   return sb.toString();
    //
    // Collectors.joining(", ") is the whole loop, separator
    // bookkeeping included.
    // Expected: "Alice, Bob, Charlie, ..."
    // --------------------------------------------------------
    public String allNamesCommaSeparated() {
        // TODO: rewrite as a stream
        return null;
    }

    // --------------------------------------------------------
    // C7 — the names of the people in each department
    // --------------------------------------------------------
    // THE LOOP:
    //
    //   Map<String, List<String>> result = new HashMap<>();
    //   for (Employee e : employees) {
    //       result.computeIfAbsent(e.department(), k -> new ArrayList<>())
    //             .add(e.name());
    //   }
    //   return result;
    //
    // Collectors.groupingBy takes the key, and a second
    // "downstream" collector saying what to do with each group.
    // Here that is Collectors.mapping(..., Collectors.toList()).
    //
    // Expected: {Engineering=[Alice, Bob, Diana, Henry],
    //            HR=[Charlie, Frank, Jack],
    //            Marketing=[Eve, Grace, Ivy]}
    // --------------------------------------------------------
    public Map<String, List<String>> namesByDepartment() {
        // TODO: rewrite as a stream
        return null;
    }
}
