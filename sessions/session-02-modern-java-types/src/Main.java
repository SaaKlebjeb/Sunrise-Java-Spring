/**
 * STEP 6 — run it.
 *
 * Nothing to fill in here; this file is complete and is marked GIVEN.
 * Use it to check your work by eye before you run Verify.
 *
 * Expected output:
 *
 *   RECEIPT
 *   -------
 *   Customer: alice@example.com
 *   Total:    $250.00
 *   Status:   Approved: TX-1001
 *
 *   RECEIPT
 *   -------
 *   Customer: rich@example.com
 *   Total:    $5000.00
 *   Status:   Declined: Amount exceeds limit
 *
 *   RECEIPT
 *   -------
 *   Customer: nostock@example.com
 *   Total:    $50.00
 *   Status:   Only 0 left of SKU-001
 *
 *   --- the same first order, as JSON ---
 *   {
 *     "customer": "alice@example.com",
 *     "total": 250.00,
 *     "status": "Approved: TX-1001"
 *   }
 *
 *   Discounted order: Order[customerEmail=alice@example.com, total=225.0]
 *   The original:     Order[customerEmail=alice@example.com, total=250.0]
 *   Same value? true
 */
public class Main {

    public static void main(String[] args) {

        PaymentGateway gateway = new FakePaymentGateway();
        var reporter = new OrderReporter();

        var orders = new Order[] {
                new Order("Alice@Example.com ", 250.00),   // note: mixed case + trailing space
                new Order("rich@example.com", 5000.00),
                new Order("nostock@example.com", 50.00)
        };

        for (var order : orders) {
            PaymentResult result = gateway.charge(order);
            System.out.println(reporter.receipt(order, result));
        }

        // Step 5 — the same data, escape-free, as JSON.
        System.out.println("--- the same first order, as JSON ---");
        System.out.println(reporter.summaryJson(orders[0], gateway.charge(orders[0])));
        System.out.println();

        // Immutability: withDiscount returns a NEW order, it does not modify one.
        var original = new Order("alice@example.com", 250.00);
        var discounted = original.withDiscount(10);
        System.out.println("Discounted order: " + discounted);
        System.out.println("The original:     " + original);

        // Value semantics: two separately-constructed records with the same
        // components are equal. The Session 1 class printed false here.
        var a = new Order("alice@example.com", 250.00);
        var b = new Order("alice@example.com", 250.00);
        System.out.println("Same value? " + a.equals(b));
    }
}
