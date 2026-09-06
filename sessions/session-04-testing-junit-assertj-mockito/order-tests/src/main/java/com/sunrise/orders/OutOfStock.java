package com.sunrise.orders;

/** GIVEN. We cannot fulfil it; this many are left. */
public record OutOfStock(int available) implements PaymentResult {
}
