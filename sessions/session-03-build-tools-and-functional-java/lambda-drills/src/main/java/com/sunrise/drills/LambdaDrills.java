package com.sunrise.drills;

import com.sunrise.drills.model.Employee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * ============================================================
 * DRILL SET A — Lambdas
 * ============================================================
 * Every drill below shows you code that already works, written
 * the way Java forced you to write it before Java 8: an
 * anonymous inner class, or a loop.
 *
 * Your job is to return the same behaviour as a lambda or a
 * method reference. Each one fits on a single line.
 *
 * Run LambdaDrillsTest to check your answers.
 * ============================================================
 */
public class LambdaDrills {

    /**
     * A functional interface of your own. One abstract method, so a
     * lambda can implement it. Used by drill A6.
     */
    @FunctionalInterface
    public interface Discount {
        double applyTo(double amount);
    }

    // --------------------------------------------------------
    // A1 — a Predicate that is true for even numbers
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Predicate<Integer>() {
    //       @Override
    //       public boolean test(Integer n) {
    //           return n % 2 == 0;
    //       }
    //   };
    // --------------------------------------------------------
    public Predicate<Integer> isEven() {
        // TODO: return the same thing as a lambda
        return null;
    }

    // --------------------------------------------------------
    // A2 — a Function that upper-cases a String and adds "!"
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Function<String, String>() {
    //       @Override
    //       public String apply(String text) {
    //           return text.toUpperCase() + "!";
    //       }
    //   };
    // --------------------------------------------------------
    public Function<String, String> shout() {
        // TODO: return the same thing as a lambda
        return null;
    }

    // --------------------------------------------------------
    // A3 — a Consumer that appends " done" to a StringBuilder
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Consumer<StringBuilder>() {
    //       @Override
    //       public void accept(StringBuilder sb) {
    //           sb.append(" done");
    //       }
    //   };
    //
    // A Consumer takes a value and returns nothing.
    // --------------------------------------------------------
    public Consumer<StringBuilder> markDone() {
        // TODO: return the same thing as a lambda
        return null;
    }

    // --------------------------------------------------------
    // A4 — a Supplier that produces a NEW empty ArrayList each call
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Supplier<List<String>>() {
    //       @Override
    //       public List<String> get() {
    //           return new ArrayList<>();
    //       }
    //   };
    //
    // A Supplier takes nothing and returns a value.
    // Two calls to get() must hand back two different lists.
    // A constructor reference does this in four characters.
    // --------------------------------------------------------
    public Supplier<List<String>> newList() {
        // TODO: return the same thing as a lambda or a constructor reference
        return null;
    }

    // --------------------------------------------------------
    // A5 — a Comparator that orders employees by salary, highest first
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Comparator<Employee>() {
    //       @Override
    //       public int compare(Employee a, Employee b) {
    //           return Double.compare(b.salary(), a.salary());
    //       }
    //   };
    //
    // A lambda will do. Comparator.comparingDouble(...).reversed()
    // reads better still.
    // --------------------------------------------------------
    public Comparator<Employee> bySalaryHighestFirst() {
        // TODO: return the same thing as a lambda, or build it with Comparator
        return null;
    }

    // --------------------------------------------------------
    // A6 — your own functional interface, implemented by a lambda
    // --------------------------------------------------------
    // Return a Discount that takes 10% off any amount.
    // applyTo(200.0) must give 180.0.
    //
    // Discount is declared at the top of this file. It has exactly
    // one abstract method, which is the only thing a lambda needs.
    // --------------------------------------------------------
    public Discount tenPercentOff() {
        // TODO: return a lambda implementing Discount
        return null;
    }

    // --------------------------------------------------------
    // A7 — capture a parameter inside a lambda (a closure)
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Supplier<String>() {
    //       @Override
    //       public String get() {
    //           return "Hello, " + name;
    //       }
    //   };
    //
    // The lambda you write reads `name` from the enclosing method.
    // That works because `name` is never reassigned — it is
    // "effectively final". Try adding `name = "x";` above your
    // return and read the error.
    // --------------------------------------------------------
    public Supplier<String> greeter(String name) {
        // TODO: return the same thing as a lambda
        return null;
    }

    // --------------------------------------------------------
    // A8 — combine two Predicates instead of writing a third
    // --------------------------------------------------------
    // THE OLD WAY:
    //
    //   return new Predicate<Employee>() {
    //       @Override
    //       public boolean test(Employee e) {
    //           return e.department().equals("Engineering")
    //               && e.yearsOfExperience() >= 5;
    //       }
    //   };
    //
    // Write the two halves as separate Predicates and join them
    // with .and(...). That is the point of this drill — a single
    // fused lambda passes the test but misses the lesson.
    // --------------------------------------------------------
    public Predicate<Employee> seniorEngineer() {
        // TODO: build two Predicates and combine them with .and(...)
        return null;
    }
}
