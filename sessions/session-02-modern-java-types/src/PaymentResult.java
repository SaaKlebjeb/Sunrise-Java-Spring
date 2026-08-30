/**
 * STEP 2 — A closed set of outcomes.
 *
 * Charging an order ends exactly three ways. Not "three ways we know about" —
 * three ways, full stop. `sealed` is how you tell the compiler that.
 *
 * TODO: make this a sealed interface permitting exactly Approved, Declined
 *       and OutOfStock:
 *
 *   public sealed interface PaymentResult
 *           permits Approved, Declined, OutOfStock { }
 *
 * Read it as a sentence: "a PaymentResult is an Approved, a Declined,
 * or an OutOfStock — and nothing else, ever."
 *
 * Note the order you have to work in: `permits` names three types, so all
 * three files below must exist and must implement this interface before
 * anything compiles. Do all four files, then compile.
 */
public interface PaymentResult {
}
