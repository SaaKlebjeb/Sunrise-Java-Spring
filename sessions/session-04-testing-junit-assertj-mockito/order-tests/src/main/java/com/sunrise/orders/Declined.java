package com.sunrise.orders;

/** GIVEN. The bank said no. */
public record Declined(String reason) implements PaymentResult {
}
