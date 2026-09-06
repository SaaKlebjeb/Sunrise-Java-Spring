package com.sunrise.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ============================================================
 * PART TWO — testing a calculation
 * ============================================================
 * `OrderReporter` produces four numbers, two of which are
 * doubles that mean different things. Nothing in the type
 * system stops them being swapped. Your tests do.
 *
 * @BeforeEach and the sample orders are written for you.
 * One test is written for you. Write the four marked TODO —
 * each one calls fail(...) until you do. Delete that line as
 * you write the test.
 * ============================================================
 */
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

    // ---- GIVEN, as a model. Do not change it. ----
    @Test
    @DisplayName("counts the orders")
    void countsTheOrders() {
        SalesReport report = reporter.summarise(orders);

        assertThat(report.orderCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("adds the totals up")
    void totalsTheRevenue() {
        // TODO: work out the expected revenue from the three orders above
        //       by hand, then assert it. Do not compute it in the test —
        //       a test that recomputes the logic tests nothing.
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("divides revenue by the number of orders")
    void averagesTheOrderValue() {
        // TODO: the average will not be exact. within(0.01) is fine.
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("names the customer who spent the most in total, not on one order")
    void findsTheTopCustomer() {
        // TODO: read the sample data above carefully before you assume
        //       the answer. One customer placed the single biggest order;
        //       a different one spent the most in total.
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("returns a zero report for an empty list, and does not throw")
    void handlesAnEmptyList() {
        // TODO: summarise(List.of()) and assert all four fields.
        //       This is the test most people skip. It is also the one
        //       that catches the most bugs.
        fail("TODO: write this test");
    }
}
