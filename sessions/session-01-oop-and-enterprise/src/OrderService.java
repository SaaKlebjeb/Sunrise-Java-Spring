/**
 * ============================================================================
 *  THIS FILE HAS TWO EDITS. They are the heart of the exercise.
 * ============================================================================
 *
 *  As written, this class builds its own collaborators. That one decision means:
 *    - it cannot be tested without a real MySQL server and a real mail server
 *    - changing the database forces an edit here
 *    - changing the mail provider forces an edit here
 *
 *  Everything else in this folder is already written for you. You only have to
 *  change how this class GETS its two collaborators.
 *
 * ----------------------------------------------------------------------------
 *  EDIT 1 — change the two field types to the INTERFACES, and delete the `new`.
 *
 *      before:  private final MySqlOrderRepository repository = new MySqlOrderRepository();
 *      after:   private final OrderRepository repository;
 *
 *      Do the same for the email sender:  EmailSender emailSender;
 *
 *      When you are done, the word `new` must not appear anywhere in this file.
 *      That is the mechanical test for whether the inversion is complete.
 *
 * ----------------------------------------------------------------------------
 *  EDIT 2 — add a constructor that receives both collaborators:
 *
 *      public OrderService(OrderRepository repository, EmailSender emailSender) {
 *          this.repository = repository;
 *          this.emailSender = emailSender;
 *      }
 *
 *      That is it. placeOrder() below does not change at all — notice that.
 *      The logic was never the problem; only where the collaborators came from.
 * ============================================================================
 */
public class OrderService {

    // TODO (EDIT 1): replace these two lines with interface-typed final fields
    private final OrderRepository repository;
    private final EmailSender emailSender;

    // TODO (EDIT 2): add the constructor here
    public OrderService(OrderRepository repository,EmailSender emailSender){
        this.repository = repository;
        this.emailSender =emailSender;
    }


    // Unchanged. This method is already correct and stays exactly as it is.
    public void placeOrder(Order order) {
        repository.save(order);
        emailSender.send(order.customerEmail(), "Order confirmed");
    }
}
