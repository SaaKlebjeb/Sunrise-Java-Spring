/**
 * STEPS 3, 4 and 5 — SOLUTION.
 */
public class OrderReporter {

    /**
     * STEP 3 — instanceof with a pattern variable. No casts anywhere.
     *
     * The final throw is unreachable: PaymentResult is sealed and all three
     * permitted outcomes are handled above. It is here only because the
     * compiler cannot yet see that. Next session this whole body becomes one
     * exhaustive switch and the dead line goes away.
     */
    public String describe(PaymentResult result) {
        if (result instanceof Approved a) {
            return "Approved: " + a.transactionId();
        }
        if (result instanceof Declined d) {
            return "Declined: " + d.reason();
        }
        if (result instanceof OutOfStock o) {
            return "Only " + o.available() + " left of " + o.sku();
        }
        throw new IllegalStateException("unknown outcome: " + result);
    }

    /**
     * STEP 4 — a text block with a trailing newline.
     *
     * The closing """ sits on its own line, in the same column as the
     * content, so: indentation stripped, one \n at the end.
     */
    public String receipt(Order order, PaymentResult result) {
        return """
                RECEIPT
                -------
                Customer: %s
                Total:    $%.2f
                Status:   %s
                """.formatted(order.customerEmail(), order.total(), describe(result));
    }

    /**
     * STEP 5 — a text block with NO trailing newline.
     *
     * The closing """ is tucked onto the end of the last content line. That is
     * the only difference from receipt() above, and it is worth one character.
     *
     * Note also: not a single escaped quote. That is the whole reason text
     * blocks and JSON belong together.
     */
    public String summaryJson(Order order, PaymentResult result) {
        return """
                {
                  "customer": "%s",
                  "total": %.2f,
                  "status": "%s"
                }""".formatted(order.customerEmail(), order.total(), describe(result));
    }
}
