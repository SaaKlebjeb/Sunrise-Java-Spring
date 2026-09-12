package com.sunrise.drills;

import com.sunrise.drills.data.SampleData;
import com.sunrise.drills.model.Employee;

import java.util.Comparator;
import java.util.List;

/** REFERENCE SOLUTION — drill set B. */
public class StreamDrills {

    private final List<Employee> employees = SampleData.employees();
    private final List<Integer> numbers = SampleData.numbers();
    private final List<String> names = SampleData.names();

    public List<Integer> evenNumbers() {
        return numbers.stream().filter(n -> n % 2 == 0).toList();
    }

    public List<String> shoutedNames() {
        return names.stream().map(String::toUpperCase).toList();
    }

    public List<String> employeeNames() {
        return employees.stream().map(Employee::name).toList();
    }

    public List<Employee> highEarners() {
        return employees.stream().filter(e -> e.salary() > 70000).toList();
    }

    public List<Integer> sortedNumbers() {
        return numbers.stream().sorted().toList();
    }

    public List<String> topThreeEarnerNames() {
        return employees.stream()
                .sorted(Comparator.comparingDouble(Employee::salary).reversed())
                .limit(3)
                .map(Employee::name)
                .toList();
    }

    public List<String> distinctCitiesSorted() {
        return employees.stream().map(Employee::city).distinct().sorted().toList();
    }
}
