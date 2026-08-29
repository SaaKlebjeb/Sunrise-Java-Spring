/**
 * BLAST RADIUS — what `private` actually buys you.
 *
 * Run it:   from a terminal in this folder --   java BlastRadius.java
 *            (One file, no compile step. Up-arrow + Enter to re-run.)
 *
 * Two classes hold the same information: an order's total.
 * One exposes the field. One hides it behind a method.
 * Today they behave identically. The difference only shows up
 * when the requirement changes — which is the whole point.
 *
 * DO THE EXPERIMENT AT THE BOTTOM OF THIS FILE. Reading it is not enough.
 */
public class BlastRadius {

    // ------------------------------------------------------------------
    // Version 1 — the field is public. Anyone can read it, anyone can
    // write it, and everyone who does is now coupled to the fact that
    // a total is "a double, in dollars".
    // ------------------------------------------------------------------
    static class OpenOrder {
        public String customerEmail;
        public double total;          // dollars, as a double

        OpenOrder(String customerEmail, double total) {
            this.customerEmail = customerEmail;
            this.total = total;
        }
    }

    // ------------------------------------------------------------------
    // Version 2 — the field is private. Callers ask a question
    // ("what is the total?") instead of reading a variable.
    //
    // Note what has ALREADY happened here: this class stores money as a
    // whole number of cents, because doubles lose pennies. Nobody outside
    // this class had to be told. That change is invisible from the outside.
    // ------------------------------------------------------------------
    static class ClosedOrder {
        private final String customerEmail;
        private final long totalCents;    // cents, as a long

        ClosedOrder(String customerEmail, double totalInDollars) {
            if (customerEmail == null || !customerEmail.contains("@")) {
                throw new IllegalArgumentException("not an email: " + customerEmail);
            }
            if (totalInDollars < 0) {
                throw new IllegalArgumentException("total cannot be negative");
            }
            this.customerEmail = customerEmail;
            this.totalCents = Math.round(totalInDollars * 100);
        }

        String customerEmail() { return customerEmail; }
        double total()         { return totalCents / 100.0; }
    }

    // ------------------------------------------------------------------
    // Four "callers". Each one is a separate place in a real codebase.
    // Watch how differently they reach for the total.
    // ------------------------------------------------------------------

    static String openReceipt(OpenOrder o)   { return o.customerEmail + " owes $" + o.total; }
    static String closedReceipt(ClosedOrder o) { return o.customerEmail() + " owes $" + o.total(); }

    static boolean openNeedsApproval(OpenOrder o)   { return o.total > 1000; }
    static boolean closedNeedsApproval(ClosedOrder o) { return o.total() > 1000; }

    static double openWithTax(OpenOrder o)   { return o.total * 1.10; }
    static double closedWithTax(ClosedOrder o) { return o.total() * 1.10; }

    static String openShipping(OpenOrder o)   { return o.total > 50 ? "free" : "$4.99"; }
    static String closedShipping(ClosedOrder o) { return o.total() > 50 ? "free" : "$4.99"; }

    public static void main(String[] args) {
        OpenOrder open = new OpenOrder("alice@example.com", 250.00);
        ClosedOrder closed = new ClosedOrder("alice@example.com", 250.00);

        System.out.println();
        System.out.println("Same information, two designs");
        System.out.println("=============================");
        System.out.println();
        System.out.println("  public field   -> " + openReceipt(open));
        System.out.println("  private field  -> " + closedReceipt(closed));
        System.out.println();
        System.out.println("  approval?      -> " + openNeedsApproval(open)
                                                  + " / " + closedNeedsApproval(closed));
        System.out.println("  with tax       -> " + openWithTax(open)
                                                  + " / " + closedWithTax(closed));
        System.out.println("  shipping       -> " + openShipping(open)
                                                  + " / " + closedShipping(closed));
        System.out.println();
        System.out.println("Identical behaviour. So far the design choice looks free.");
        System.out.println();

        // --------------------------------------------------------------
        // Where the two designs stop being equivalent: INVARIANTS.
        // --------------------------------------------------------------
        System.out.println("Now break one of them");
        System.out.println("=====================");
        System.out.println();

        open.total = -9999;               // nothing stops this. Nothing at all.
        open.customerEmail = "not-an-email";
        System.out.println("  OpenOrder after someone assigns nonsense:");
        System.out.println("    " + openReceipt(open));
        System.out.println("    approval? " + openNeedsApproval(open) + "   (a negative order)");
        System.out.println();

        try {
            new ClosedOrder("not-an-email", -9999);
        } catch (IllegalArgumentException e) {
            System.out.println("  ClosedOrder refuses to exist in that state:");
            System.out.println("    " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println();
        System.out.println("  A public field cannot check anything. A constructor can.");
        System.out.println("  `private` is what makes the check unavoidable.");
        System.out.println();

        System.out.println("---------------------------------------------------");
        System.out.println("  NOW DO THE EXPERIMENT. See the comment at the");
        System.out.println("  bottom of BlastRadius.java. It takes two minutes");
        System.out.println("  and it is the point of the whole file.");
        System.out.println("---------------------------------------------------");
        System.out.println();
    }
}

/*
 * ===================================================================
 * THE EXPERIMENT  (two minutes — actually do it)
 * ===================================================================
 *
 * A requirement arrives:
 *
 *     "Stop storing money as a double. We are losing pennies on
 *      large orders. Store whole cents."
 *
 * ClosedOrder already survived this change — look at it: it stores
 * `long totalCents` and nobody outside noticed.
 *
 * Now inflict the same change on OpenOrder:
 *
 *   1. In OpenOrder, change
 *          public double total;
 *      to
 *          public long totalCents;
 *
 *   2. Compile it:   javac BlastRadius.java
 *
 *   3. COUNT THE COMPILE ERRORS. javac lists every one and then
 *      prints the total. Every one of them is a line somewhere else
 *      in the program that has to be opened and edited because of a
 *      decision made inside OpenOrder.
 *
 *   4. Now do the equivalent to ClosedOrder — change how it stores
 *      the total internally, but keep `double total()` returning
 *      dollars. Count the errors outside the class.
 *
 * That number — errors outside the class — is the BLAST RADIUS.
 * `private` is the tool that keeps it at zero.
 *
 * This file has four callers. Real systems have four hundred.
 * ===================================================================
 */
