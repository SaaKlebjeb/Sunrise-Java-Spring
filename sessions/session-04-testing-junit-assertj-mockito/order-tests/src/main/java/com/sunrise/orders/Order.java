package com.sunrise.orders;

/**
 * A customer order.
 *
 * GIVEN — complete and working. Do not change it; your job is to test it.
 *
 * The compact constructor does two different jobs, and a good test suite
 * checks both:
 *   - it VALIDATES, so an invalid Order can never exist
 *   - it NORMALISES, so two orders that mean the same thing are equal
 */
public record Order(String customerEmail, double total, int quantity) {

    public Order {
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new IllegalArgumentException("customerEmail must contain @");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        customerEmail = customerEmail.trim().toLowerCase();
    }

    /** A copy of this order with a percentage taken off the total. */
    public Order withDiscount(double percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        return new Order(customerEmail, total * (1 - percent / 100), quantity);
    }
}
