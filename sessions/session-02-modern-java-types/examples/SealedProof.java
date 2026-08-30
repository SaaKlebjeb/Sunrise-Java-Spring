/**
 * Sealed types - one record with a field for every case, then a closed set.
 *
 * Run:  java SealedProof.java
 */

public class SealedProof {

    // ---------- BEFORE ----------
    record StatusBlob(boolean ok, String errorCode, Integer available) { }

    // ---------- AFTER ----------
    // Replace the blob with a closed set: a PaymentResult is one of exactly
    // these three, and the compiler will not let anyone add a fourth.
    sealed interface PaymentResult permits Approved, Declined, OutOfStock { }

    record Approved(String transactionId)        implements PaymentResult { }
    record Declined(String reason)               implements PaymentResult { }
    record OutOfStock(String sku, int available) implements PaymentResult { }

    public static void main(String[] args) {

        System.out.println("BEFORE - one record with a field for every case");
        System.out.println("  " + new StatusBlob(true, "DECLINED", null));
        System.out.println("     ^ succeeded AND declined at the same time");
        System.out.println("  " + new StatusBlob(false, null, null));
        System.out.println("     ^ failed, and will not say why");

        System.out.println();
        System.out.println("AFTER - three records, nothing else allowed");
        System.out.println("  " + new Approved("TX-1001"));
        System.out.println("  " + new Declined("Amount exceeds limit"));
        System.out.println("  " + new OutOfStock("SKU-001", 0));
        System.out.println();
        System.out.println("  Neither state above can be built out of these three.");

        System.out.println();
        System.out.println("USING IT TODAY");
        System.out.println("  describe(new Approved(\"TX-1001\"))  ->  " + describe(new Approved("TX-1001")));
        System.out.println("  ...but look at the last line of describe(): a throw that can never run.");
    }

    static String describe(PaymentResult result) {
        if (result instanceof Approved a)   return "Approved: " + a.transactionId();
        if (result instanceof Declined d)   return "Declined: " + d.reason();
        if (result instanceof OutOfStock o) return "Only " + o.available() + " left of " + o.sku();
        throw new IllegalStateException("unknown outcome");   // unreachable, and the compiler makes you write it
    }
}

/*
 * LIVE DEMO - the high point of the session
 *
 *   1. Add next to the other three records, on screen:
 *          record Refunded(String transactionId) implements PaymentResult { }
 *      Then `javac SealedProof.java` and read the error verbatim:
 *          error: class is not allowed to extend sealed class: PaymentResult
 *                 (as it is not listed in its 'permits' clause)
 *
 *   2. Now add Refunded to the permits clause. It compiles - and nothing warns
 *      you that describe() has no branch for it. Ask the room what happens when
 *      a Refunded reaches describe(). That itch is what next session scratches.
 *
 *   Undo both before the next run.
 */
