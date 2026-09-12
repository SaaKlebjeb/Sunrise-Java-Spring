package com.sunrise.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** REFERENCE SOLUTION — 5 class tests (1 given + 4 written) and 1 homework test. */
@DisplayName("OrderReporter")
class OrderReporterTest {

    private OrderReporter reporter;
    private List<Order> orders;

    @BeforeEach
    void setUp() {
        reporter = new OrderReporter();
        orders = List.of(
                new Order("chan@example.com", 100.00, 1),
                new Order("sokha@example.com", 250.00, 2),
                new Order("chan@example.com", 200.00, 1));
    }

    @Test
    @DisplayName("counts the orders")
    void countsTheOrders() {
        SalesReport report = reporter.summarise(orders);

        assertThat(report.orderCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("adds the totals up")
    void totalsTheRevenue() {
        SalesReport report = reporter.summarise(orders);

        assertThat(report.totalRevenue()).isEqualTo(550.00, within(0.001));
    }

    @Test
    @DisplayName("divides revenue by the number of orders")
    void averagesTheOrderValue() {
        SalesReport report = reporter.summarise(orders);

        assertThat(report.averageOrderValue()).isEqualTo(183.33, within(0.01));
    }

    @Test
    @DisplayName("names the customer who spent the most in total, not on one order")
    void findsTheTopCustomer() {
        SalesReport report = reporter.summarise(orders);

        assertThat(report.topCustomer()).isEqualTo("chan@example.com");
    }

    @Test
    @DisplayName("returns a zero report for an empty list, and does not throw")
    void handlesAnEmptyList() {
        SalesReport report = reporter.summarise(List.of());

        assertThat(report.orderCount()).isZero();
        assertThat(report.totalRevenue()).isEqualTo(0.0, within(0.001));
        assertThat(report.averageOrderValue()).isEqualTo(0.0, within(0.001));
        assertThat(report.topCustomer()).isEqualTo("none");
    }

    // HOMEWORK 2 — this answer pins down "null means empty". Throwing is equally defensible.
    @Test
    @DisplayName("treats a null list like an empty one")
    void handlesANullList() {
        SalesReport report = reporter.summarise(null);

        assertThat(report).isEqualTo(new SalesReport(0, 0.0, 0.0, "none"));
    }
}
