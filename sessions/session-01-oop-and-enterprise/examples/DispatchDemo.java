/**
 * DISPATCH DEMO — what is polymorphic, and what only looks like it.
 *
 * Run it:   from a terminal in this folder --   java DispatchDemo.java
 *            (One file, no compile step. Up-arrow + Enter to re-run.)
 *
 * Before you run it, PREDICT each answer. Write your predictions down.
 * The three surprises in the second half catch almost everybody, and getting
 * them wrong on paper is how you remember them.
 */
public class DispatchDemo {

    // =================================================================
    // WHAT WORKS — instance methods ARE polymorphic. This is the one you
    // want, and the one the whole course is built on.
    // =================================================================

    interface EmailSender {
        void send(String to, String message);
    }

    static class SmtpEmailSender implements EmailSender {
        public void send(String to, String message) {
            System.out.println("    [SMTP]    really emailing " + to + ": " + message);
        }
    }

    static class ConsoleEmailSender implements EmailSender {
        public void send(String to, String message) {
            System.out.println("    [CONSOLE] pretending to email " + to + ": " + message);
        }
    }

    /**
     * Look closely: this method has NO IDEA which class it is talking to.
     * It cannot find out and it does not care. That ignorance is the feature.
     */
    static void notifyCustomer(EmailSender sender) {
        sender.send("alice@example.com", "Order confirmed");
    }

    // =================================================================
    // WHAT DOESN'T - three things that are NOT polymorphic.
    // Each one is chosen by the DECLARED type, at compile time.
    // =================================================================

    static class Parent {
        String label = "PARENT field";              // fields: not polymorphic
        static String describe() { return "PARENT static"; }   // statics: not polymorphic
        String name() { return "PARENT method"; }              // instance method: IS polymorphic
        private String secret() { return "PARENT private"; }   // private: not polymorphic
        String callSecret() { return secret(); }
    }

    static class Child extends Parent {
        String label = "CHILD field";               // HIDES the parent field, does not override it
        static String describe() { return "CHILD static"; }    // HIDES, does not override
        @Override String name() { return "CHILD method"; }     // genuinely overrides
        private String secret() { return "CHILD private"; }    // a different method entirely
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("WHAT WORKS - the same line, two behaviours");
        System.out.println("==========================================");
        System.out.println();

        EmailSender sender = new SmtpEmailSender();
        System.out.println("  declared EmailSender, holding SmtpEmailSender:");
        notifyCustomer(sender);

        sender = new ConsoleEmailSender();
        System.out.println("  declared EmailSender, holding ConsoleEmailSender:");
        notifyCustomer(sender);

        System.out.println();
        System.out.println("  notifyCustomer() was never edited, never recompiled,");
        System.out.println("  and never learned that ConsoleEmailSender exists.");
        System.out.println();

        // -------------------------------------------------------------
        System.out.println("WHAT DOESN'T - three things that are NOT polymorphic");
        System.out.println("====================================================");
        System.out.println();

        Parent asParent = new Child();   // declared Parent, actual object Child
        Child  asChild  = new Child();

        System.out.println("  Parent asParent = new Child();");
        System.out.println();

        System.out.println("  1. INSTANCE METHOD  asParent.name()   -> " + asParent.name());
        System.out.println("     the ACTUAL object decides.  This is polymorphism working.");
        System.out.println();

        System.out.println("  2. FIELD            asParent.label    -> " + asParent.label);
        System.out.println("                      asChild.label     -> " + asChild.label);
        System.out.println("     the DECLARED type decides. Fields are HIDDEN, never overridden.");
        System.out.println("     Same object, two answers, depending on the variable you hold it in.");
        System.out.println();

        System.out.println("  3. STATIC METHOD    Parent.describe() -> " + Parent.describe());
        System.out.println("                      Child.describe()  -> " + Child.describe());
        System.out.println("     chosen by the CLASS NAME, not by any object. No dispatch at all.");
        System.out.println();

        System.out.println("  4. PRIVATE METHOD   asParent.callSecret() -> " + asParent.callSecret());
        System.out.println("     `secret()` is private, so Child's version does not override it.");
        System.out.println("     Parent calls Parent's, even though the object is a Child.");
        System.out.println();

        System.out.println("---------------------------------------------------");
        System.out.println("  RULE OF THUMB");
        System.out.println();
        System.out.println("  Only non-private, non-static INSTANCE METHODS are");
        System.out.println("  dispatched on the actual object. Everything else is");
        System.out.println("  resolved from the declared type at compile time.");
        System.out.println();
        System.out.println("  This is why enterprise Java puts its behaviour in");
        System.out.println("  instance methods behind interfaces - it is the only");
        System.out.println("  part of the language you can swap out from outside.");
        System.out.println("---------------------------------------------------");
        System.out.println();
    }
}

/*
 * ===================================================================
 * THINGS TO TRY
 * ===================================================================
 *
 * 1. Add a third EmailSender that writes to a file. How many existing
 *    lines do you have to change to use it?  (Answer: one — the `new`.)
 *
 * 2. Delete `@Override` from Child.name() and misspell it as `nmae()`.
 *    Run again. What does asParent.name() print now, and why?
 *    That is what `@Override` is FOR: it is a compile-time check that
 *    you are actually overriding something.
 *
 * 3. In the second half, change `Parent asParent = new Child();` to
 *    `Child asParent = new Child();` and re-run. Which of the four
 *    answers changed? Which stayed the same? Explain both.
 * ===================================================================
 */
