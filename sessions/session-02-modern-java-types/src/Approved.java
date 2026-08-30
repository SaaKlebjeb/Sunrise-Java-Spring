/**
 * STEP 2 — The success outcome. It carries a transaction id, and nothing else.
 *
 * TODO: make this a record implementing PaymentResult:
 *
 *   public record Approved(String transactionId) implements PaymentResult { }
 *
 * Note what is NOT here: no `reason` field sitting empty, no `available`
 * field set to null. Each outcome carries exactly its own data.
 */
public class Approved {
}
