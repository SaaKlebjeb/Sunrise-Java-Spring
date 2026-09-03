/**
 * STEP 1 — SOLUTION.
 *
 * Twelve lines became one, and this version has more in it: equals(),
 * hashCode() and toString() were all generated.
 */
public record Order(String customerEmail, double total) {

    // Compact constructor: no parameter list, no field assignment.
    // Your code runs first; the compiler assigns the fields afterwards.
    public Order {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail is required");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
        customerEmail = customerEmail.trim().toLowerCase();
    }

    /** A "wither": returns a NEW Order. Records are never modified. */
    public Order withDiscount(double percent) {
        return new Order(customerEmail, total * (1 - percent / 100));
    }
}
