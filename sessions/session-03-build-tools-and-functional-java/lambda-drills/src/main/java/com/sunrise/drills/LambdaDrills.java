package com.sunrise.drills;

import com.sunrise.drills.model.Employee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** REFERENCE SOLUTION — drill set A. */
public class LambdaDrills {

    @FunctionalInterface
    public interface Discount {
        double applyTo(double amount);
    }

    public Predicate<Integer> isEven() {
        return n -> n % 2 == 0;
    }

    public Function<String, String> shout() {
        return text -> text.toUpperCase() + "!";
    }

    public Consumer<StringBuilder> markDone() {
        return sb -> sb.append(" done");
    }

    public Supplier<List<String>> newList() {
        return ArrayList::new;
    }

    public Comparator<Employee> bySalaryHighestFirst() {
        return Comparator.comparingDouble(Employee::salary).reversed();
    }

    public Discount tenPercentOff() {
        return amount -> amount * 0.9;
    }

    public Supplier<String> greeter(String name) {
        return () -> "Hello, " + name;
    }

    public Predicate<Employee> seniorEngineer() {
        Predicate<Employee> inEngineering = e -> e.department().equals("Engineering");
        Predicate<Employee> experienced = e -> e.yearsOfExperience() >= 5;
        return inEngineering.and(experienced);
    }
}
