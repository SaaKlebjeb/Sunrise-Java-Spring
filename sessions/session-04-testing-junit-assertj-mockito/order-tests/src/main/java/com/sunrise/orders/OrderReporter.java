package com.sunrise.orders;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns a list of orders into a report.
 *
 * GIVEN — complete and working. Written with the streams you met last
 * session, which is deliberate: this is the class where a wrong pipeline
 * produces a plausible-looking number, and only a test notices.
 */
public class OrderReporter {

    public SalesReport summarise(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return new SalesReport(0, 0.0, 0.0, "none");
        }

        double revenue = orders.stream()
                .mapToDouble(Order::total)
                .sum();

        Map<String, Double> spendByCustomer = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::customerEmail,
                        Collectors.summingDouble(Order::total)));

        String top = spendByCustomer.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");

        return new SalesReport(
                orders.size(),
                revenue,
                revenue / orders.size(),
                top);
    }
}
