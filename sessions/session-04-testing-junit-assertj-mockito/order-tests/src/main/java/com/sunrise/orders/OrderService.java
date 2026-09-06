package com.sunrise.orders;

/**
 * Places an order: charge, then — only if the charge worked — store it and
 * tell the customer.
 *
 * GIVEN — complete and working.
 *
 * Every dependency arrives through the constructor, which is what makes this
 * class testable at all: a test can hand it three stand-ins and never touch a
 * payment provider, a database, or a mail server.
 */
public class OrderService {

    private final PaymentGateway gateway;
    private final OrderRepository repository;
    private final EmailSender emailSender;

    public OrderService(PaymentGateway gateway,
                        OrderRepository repository,
                        EmailSender emailSender) {
        this.gateway = gateway;
        this.repository = repository;
        this.emailSender = emailSender;
    }

    public PaymentResult place(Order order) {
        PaymentResult result = gateway.charge(order);

        if (result instanceof Approved approved) {
            repository.save(order);
            emailSender.sendConfirmation(order, approved.transactionId());
        }

        return result;
    }
}
