package com.sunrise.orders;

/**
 * GIVEN. A summary of a batch of orders.
 *
 * Note the order of the components. Two of them are `double`s that mean
 * completely different things, and nothing in the type system stops you
 * swapping them. A test does.
 */
public record SalesReport(
        long orderCount,
        double totalRevenue,
        double averageOrderValue,
        String topCustomer) {
}
