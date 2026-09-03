/**
 * STEP 2 — SOLUTION. Deterministic rules so the output is testable.
 */
public class FakePaymentGateway implements PaymentGateway {

    private static final double LIMIT = 1000.00;

    @Override
    public PaymentResult charge(Order order) {
        if (order.total() > LIMIT) {
            return new Declined("Amount exceeds limit");
        }
        if (order.customerEmail().startsWith("nostock")) {
            return new OutOfStock("SKU-001", 0);
        }
        return new Approved("TX-1001");
    }
}
