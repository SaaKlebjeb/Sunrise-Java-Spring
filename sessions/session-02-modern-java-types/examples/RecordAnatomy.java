/**
 * Records - the hand-written class, then the same thing as a record.
 *
 * Run:  java RecordAnatomy.java
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RecordAnatomy {

    // ---------- BEFORE ----------
    static final class OldOrder {
        private final String customerEmail;
        private final double total;

        OldOrder(String customerEmail, double total) {
            this.customerEmail = customerEmail;
            this.total = total;
        }

        String customerEmail() { return customerEmail; }
        double total()         { return total; }
    }

    // ---------- AFTER ----------
    // Delete the whole class above and write this one line:
    //
    //     record Order(String customerEmail, double total) { }
    //
    // The compact constructor below is section 2. The one line is the base.
    record Order(String customerEmail, double totalf) {
        Order {
            if (total < 0) throw new IllegalArgumentException("total cannot be negative");
            customerEmail = customerEmail.trim().toLowerCase();
        }
    }

    record Basket(String owner, List<String> items) { }

    public static void main(String[] args) {

        var oldA = new OldOrder("alice@example.com", 250.00);
        var oldB = new OldOrder("alice@example.com", 250.00);
        var oldSeen = new HashSet<OldOrder>();
        oldSeen.add(oldA);

        System.out.println("BEFORE - hand-written class");
        System.out.println("  toString            " + oldA);
        System.out.println("  a.equals(b)         " + oldA.equals(oldB));
        System.out.println("  set.contains(b)     " + oldSeen.contains(oldB));

        var a = new Order("alice@example.com", 250.00);
        var b = new Order("alice@example.com", 250.00);
        var seen = new HashSet<Order>();
        seen.add(a);

        System.out.println();
        System.out.println("AFTER - record");
        System.out.println("  toString            " + a);
        System.out.println("  a.equals(b)         " + a.equals(b));
        System.out.println("  set.contains(b)     " + seen.contains(b));

        System.out.println();
        System.out.println("2. COMPACT CONSTRUCTOR - validate on the way in");
        System.out.println("  new Order(\"  ALICE@example.com \", 250)  ->  "
                + new Order("  ALICE@example.com ", 250).customerEmail());
        try {
            new Order("a@b.com", -1);
        } catch (IllegalArgumentException e) {
            System.out.println("  new Order(\"a@b.com\", -1)               ->  rejected: " + e.getMessage());
        }

        var items = new ArrayList<String>();
        items.add("widget");
        var basket = new Basket("alice", items);

        System.out.println();
        System.out.println("3. THE TRAP - final protects the field, not the list");
        System.out.println("  built with one item     " + basket);
        items.add("SNEAKED IN");
        System.out.println("  after items.add(...)    " + basket);
    }
}

/*
 * LIVE DEMO
 *
 *   Section 3: add a compact constructor to Basket, on screen -
 *       Basket { items = List.copyOf(items); }
 *   Run again: the sneaked item no longer appears. Undo afterwards.
 */
