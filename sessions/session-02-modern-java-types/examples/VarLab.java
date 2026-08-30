/**
 * var - the same types, with less typing.
 *
 * Run:  java VarLab.java
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VarLab {

    public static void main(String[] args) {

        // ---------- BEFORE ----------
        String name = "Alice";
        double total = 250.00;
        HashMap<String, List<String>> lines = new HashMap<String, List<String>>();

        System.out.println("BEFORE - type written out");
        System.out.println("  name    " + type(name));
        System.out.println("  total   " + type(total));
        System.out.println("  lines   " + type(lines) + "   <- the type was typed twice on that line");

        // ---------- AFTER ----------
        // Replace the declared type with `var`. Nothing else on the line changes.
        var vName = "Alice";
        var vTotal = 250.00;
        var vLines = new HashMap<String, List<String>>();

        System.out.println();
        System.out.println("AFTER - var");
        System.out.println("  name    " + type(vName));
        System.out.println("  total   " + type(vTotal));
        System.out.println("  lines   " + type(vLines));
        System.out.println();
        System.out.println("  Identical. The type is in the class file either way,");
        System.out.println("  and `vName = 42;` still does not compile.");

        // ---------- the one gotcha ----------
        var loose = new ArrayList<>();   // no type on the left, so the diamond gives Object
        loose.add("hello");
        loose.add(42);

        System.out.println();
        System.out.println("GOTCHA - var with a bare <>");
        System.out.println("  var b = new ArrayList<>();  b now holds  " + loose);
        System.out.println("  Write new ArrayList<String>() instead.");
    }

    static String type(Object o) {
        return o.getClass().getSimpleName();
    }
}

/*
 * LIVE DEMO
 *
 *   Add one of these, run `javac VarLab.java`, read the error out loud:
 *       var count = 0;      (as a field)
 *       var nothing;        (inside main)
 *       var empty = null;   (inside main)
 *   One reason for all three: `var` copies a type the compiler can already
 *   see. Undo afterwards.
 */
