import java.util.ArrayList;
import java.util.List;

/**
 * GIVEN — nothing to do here, but this class is the punchline of the session.
 *
 * A SECOND way to keep the OrderRepository promise, with no database at all.
 *
 * It already exists and it already works. In EDIT 4 you will make the
 * application use it — and you will not touch OrderService.java to do it.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final List<Order> orders = new ArrayList<>();

    @Override
    public void save(Order order) {
        orders.add(order);
        System.out.printf("[Memory] Stored order %d for %s%n",
                          orders.size(), order.customerEmail());
    }

    public int count() {
        return orders.size();
    }
}
