package com.sunrise.orders;

/**
 * GIVEN. Charges a card.
 *
 * In production this calls a payment provider over the network. In a test
 * it never should — which is the whole reason this is an interface.
 */
public interface PaymentGateway {
    PaymentResult charge(Order order);
}
