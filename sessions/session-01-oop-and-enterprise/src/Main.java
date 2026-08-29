/**
 * ============================================================================
 *  THIS FILE HAS TWO EDITS.
 * ============================================================================
 *
 *  Main is the ASSEMBLER (also called the composition root): the one class in
 *  the whole program allowed to know the concrete types. Everything else talks
 *  to interfaces.
 *
 *  Once you meet Spring, the container takes over this job — and the
 *  rest of the design does not change at all. Remember that when you get there.
 * ============================================================================
 */
public class Main {

    public static void main(String[] args) {

        // --------------------------------------------------------------
        //  EDIT 3 — wire the real application.
        //
        //  Uncomment the four lines below. Note the variables are declared
        //  with the INTERFACE type, not the concrete class. That is the habit:
        //  it stops you accidentally using a MySQL-only method later.
        // --------------------------------------------------------------

        // OrderRepository repository  = new MySqlOrderRepository();
        // EmailSender     emailSender = new SmtpOrderEmailSender();
        //
        // OrderService service = new OrderService(repository, emailSender);
        // service.placeOrder(new Order("alice@example.com", 250.00));

        // Expected output so far:
        //   [MySQL] Saving order for alice@example.com totalling $250.00
        //   [SMTP] To: alice@example.com | Order confirmed


        // --------------------------------------------------------------
        //  EDIT 4 — the payoff. Uncomment these three lines.
        //
        //  A SECOND OrderService, using InMemoryOrderRepository instead —
        //  reusing the same email sender.
        // --------------------------------------------------------------

        // OrderRepository memory = new InMemoryOrderRepository();
        // OrderService memoryService = new OrderService(memory, emailSender);
        // memoryService.placeOrder(new Order("bob@example.com", 99.00));

        // Expected additional output:
        //   [Memory] Stored order 1 for bob@example.com
        //   [SMTP] To: bob@example.com | Order confirmed


        // --------------------------------------------------------------
        //  Now answer these out loud before you run Verify:
        //
        //    Did you open OrderService.java for EDIT 4?      No.
        //    Did OrderService.java even recompile?           No.
        //
        //  You changed how this application stores orders without touching
        //  the class that places them. That is what "program to an interface"
        //  actually buys you — and it is the only thing you need to take
        //  away from today.
        // --------------------------------------------------------------
    }
}
