/**
 * GIVEN — nothing to do here. Read it, though.
 *
 * The capability: "somewhere to put an order."
 *
 * Notice what this interface does NOT say. No MySQL, no SQL, no connections,
 * no tables. Those are details of *one way* of saving an order. Naming the
 * capability separately from the mechanism is what "program to an interface"
 * means, and it is the whole point of today.
 */
public interface OrderRepository {

    void save(Order order);
}
