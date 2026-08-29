/**
 * GIVEN — nothing to do here.
 *
 * A plain data holder for an order.
 * (Later you will replace this with a `record` and delete most of it.)
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
