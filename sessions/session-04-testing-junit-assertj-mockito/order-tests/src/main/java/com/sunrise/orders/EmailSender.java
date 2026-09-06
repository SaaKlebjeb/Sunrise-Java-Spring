package com.sunrise.orders;

/** GIVEN. Sends the customer their confirmation. */
public interface EmailSender {
    void sendConfirmation(Order order, String transactionId);
}
