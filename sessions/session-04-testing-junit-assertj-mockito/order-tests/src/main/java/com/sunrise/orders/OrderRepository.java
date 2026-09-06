package com.sunrise.orders;

import java.util.List;

/** GIVEN. Where orders are stored. */
public interface OrderRepository {
    void save(Order order);

    List<Order> findAll();
}
