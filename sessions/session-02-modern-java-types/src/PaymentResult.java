/**
 * STEP 2 — SOLUTION.
 *
 * "A PaymentResult is an Approved, a Declined, or an OutOfStock —
 *  and nothing else, ever."
 */
public sealed interface PaymentResult
        permits Approved, Declined, OutOfStock { }
