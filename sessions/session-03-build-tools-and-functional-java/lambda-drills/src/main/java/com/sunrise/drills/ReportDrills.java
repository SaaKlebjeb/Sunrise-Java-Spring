package com.sunrise.drills;

import com.sunrise.drills.data.SampleData;
import com.sunrise.drills.model.Employee;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** REFERENCE SOLUTION — drill set C. */
public class ReportDrills {

    private final List<Employee> employees = SampleData.employees();

    public long engineeringHeadcount() {
        return employees.stream().filter(e -> e.department().equals("Engineering")).count();
    }

    public double totalPayroll() {
        return employees.stream().mapToDouble(Employee::salary).sum();
    }

    public double averageSalary() {
        return employees.stream().mapToDouble(Employee::salary).average().orElse(0);
    }

    public Optional<Employee> highestPaid() {
        return employees.stream().max(Comparator.comparingDouble(Employee::salary));
    }

    public boolean everyoneEarnsAtLeast(double floor) {
        return employees.stream().allMatch(e -> e.salary() >= floor);
    }

    public String allNamesCommaSeparated() {
        return employees.stream().map(Employee::name).collect(Collectors.joining(", "));
    }

    public Map<String, List<String>> namesByDepartment() {
        return employees.stream()
                .collect(Collectors.groupingBy(Employee::department,
                        Collectors.mapping(Employee::name, Collectors.toList())));
    }
}
