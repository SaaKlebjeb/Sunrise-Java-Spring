/**
 * STEP 1 — Replace this class with a record.
 *
 * Right now this is the Session 1 version: twelve lines to say
 * "an order is an email and a total" — and it is still missing
 * equals(), hashCode() and toString().
 *
 * TODO: delete everything below and write, in its place:
 *
 *   public record Order(String customerEmail, double total) { ... }
 *
 * Then add a COMPACT CONSTRUCTOR (no parameter list, no field assignment):
 *
 *   public Order {
 *       // 1. reject a null or blank email:
 *       //      throw new IllegalArgumentException("customerEmail is required")
 *       // 2. reject a negative total:
 *       //      throw new IllegalArgumentException("total cannot be negative")
 *       // 3. normalise the email: customerEmail = customerEmail.trim().toLowerCase();
 *       //    (reassigning the PARAMETER changes what gets stored — this is the
 *       //     one place in Java where that is a feature and not a bug)
 *   }
 *
 * And add one method:
 *
 *   public Order withDiscount(double percent) {
 *       // return a NEW Order — records are immutable, you never modify one
 *       // total * (1 - percent / 100)
 *   }
 *
 * When you are done, this whole file should be about 20 lines including comments.
 *
 * Stuck on what a record generates? Run ../examples/RecordAnatomy.java — it
 * prints the answer for this exact record.
 */
public class Order {

    private final String customerEmail;
    private final double total;

    public Order(String customerEmail, double total) {
        this.customerEmail = customerEmail;
        this.total = total;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public double total() {
        return total;
    }
}
