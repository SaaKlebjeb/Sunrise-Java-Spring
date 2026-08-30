/**
 * STEP 2 — One implementation of the gateway. No real payment provider;
 * the rules below are deterministic so the output is testable.
 */
public class FakePaymentGateway implements PaymentGateway {

    private static final double LIMIT = 1000.00;

    @Override
    public PaymentResult charge(Order order) {

        // TODO: apply these rules IN THIS ORDER and return the matching record.
        //
        //   1. order.total() > LIMIT
        //        -> new Declined("Amount exceeds limit")
        //
        //   2. order.customerEmail() starts with "nostock"
        //        -> new OutOfStock("SKU-001", 0)
        //
        //   3. otherwise
        //        -> new Approved("TX-1001")
        //           (a real gateway would generate an id; a fixed one keeps
        //            the exercise output stable)
        //
        // The ORDER matters, and Verify checks it: a customer who is both over
        // the limit AND a "nostock" address must come back Declined, because
        // rule 1 is tested first.

        return null; // replace this
    }
}
