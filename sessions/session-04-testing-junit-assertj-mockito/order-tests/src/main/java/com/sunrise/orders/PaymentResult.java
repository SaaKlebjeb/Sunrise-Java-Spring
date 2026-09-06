package com.sunrise.orders;

/**
 * What happened when we tried to charge for an order.
 *
 * GIVEN — complete and working.
 */
public sealed interface PaymentResult
        permits Approved, Declined, OutOfStock {
}
