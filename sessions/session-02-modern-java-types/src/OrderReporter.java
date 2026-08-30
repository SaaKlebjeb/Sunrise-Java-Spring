/**
 * STEPS 3, 4 and 5 — turning an order and an outcome into text.
 */
public class OrderReporter {

    /**
     * STEP 3 — describe an outcome.
     *
     * TODO: return the right string for each outcome, using `instanceof`
     *       with a PATTERN VARIABLE — note there is no cast on the next line,
     *       and `a` is already an Approved:
     *
     *   if (result instanceof Approved a) {
     *       return "Approved: " + a.transactionId();
     *   }
     *
     * Expected results:
     *   Approved    -> "Approved: TX-1001"
     *   Declined    -> "Declined: Amount exceeds limit"
     *   OutOfStock  -> "Only 0 left of SKU-001"
     *
     * After the three ifs you will still need something at the end, or the
     * method will not compile: not every path returns a value. Write
     *
     *   throw new IllegalStateException("unknown outcome: " + result);
     *
     * and then notice how annoying that is. You know it can never run —
     * PaymentResult is sealed and you have handled all three. The compiler
     * does not know it yet. Next session it will, and that line disappears.
     */
    public String describe(PaymentResult result) {
        return null; // replace this
    }

    /**
     * STEP 4 — build the receipt with a TEXT BLOCK. No `+` across lines.
     *
     * Target output (exactly — note the column alignment, and that it ENDS
     * WITH A NEWLINE):
     *
     *   RECEIPT
     *   -------
     *   Customer: alice@example.com
     *   Total:    $250.00
     *   Status:   Approved: TX-1001
     *
     * TODO: use a text block as the format string and fill it with
     *       .formatted(...):
     *
     *   return """
     *           RECEIPT
     *           -------
     *           Customer: %s
     *           Total:    $%.2f
     *           Status:   %s
     *           """.formatted(order.customerEmail(), order.total(), describe(result));
     *
     * Watch the indentation rule: the compiler strips the indentation of the
     * least-indented line, and the closing \"\"\" counts as a line. Keep the
     * closing delimiter in the same column as the content or every line comes
     * out with leading spaces.
     *
     * Do NOT hard-code the status line — call describe(result). Verify checks
     * this template against two different outcomes.
     */
    public String receipt(Order order, PaymentResult result) {
        return null; // replace this
    }

    /**
     * STEP 5 — the same data as JSON, and NOT ending in a newline.
     *
     * Target output (exactly — the last character is the closing brace):
     *
     *   {
     *     "customer": "alice@example.com",
     *     "total": 250.00,
     *     "status": "Approved: TX-1001"
     *   }
     *
     * TODO: another text block, with two differences from receipt():
     *
     *   1. It is full of double quotes, and NOT ONE OF THEM IS ESCAPED.
     *      That is the reason text blocks and JSON belong together — compare
     *      it with the version you would have written in Java 8:
     *          "{\"customer\":\"" + email + "\", \"total\": " + ...
     *
     *   2. It must not end in a newline. There are two ways to do that;
     *      ../examples/TextBlockLab.java shows both. Pick one and be able to
     *      say why you preferred it.
     *
     * Use %.2f for the total so it prints 250.00, not 250.0.
     */
    public String summaryJson(Order order, PaymentResult result) {
        return null; // replace this
    }
}
