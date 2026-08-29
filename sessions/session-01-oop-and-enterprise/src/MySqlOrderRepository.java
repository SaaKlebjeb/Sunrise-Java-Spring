/**
 * GIVEN — nothing to do here.
 *
 * One way to keep the OrderRepository promise. We are not opening a real
 * database; printing is enough to prove which implementation ran.
 */
public class MySqlOrderRepository implements OrderRepository {

    @Override
    public void save(Order order) {
        System.out.printf("[MySQL] Saving order for %s totalling $%.2f%n",
                          order.customerEmail(), order.total());
    }
}
