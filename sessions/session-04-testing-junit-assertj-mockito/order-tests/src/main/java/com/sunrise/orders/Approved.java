package com.sunrise.orders;

/** GIVEN. The payment went through. */
public record Approved(String transactionId) implements PaymentResult {
}
