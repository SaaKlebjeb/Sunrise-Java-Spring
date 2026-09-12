package com.sunrise.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** REFERENCE SOLUTION — 5 class tests (1 given + 4 written). */
@DisplayName("OrderService")
class OrderServiceTest {

    private PaymentGateway gateway;
    private OrderRepository repository;
    private EmailSender emailSender;
    private OrderService service;

    private final Order order = new Order("chan@example.com", 250.00, 2);

    @BeforeEach
    void setUp() {
        gateway = mock(PaymentGateway.class);
        repository = mock(OrderRepository.class);
        emailSender = mock(EmailSender.class);
        service = new OrderService(gateway, repository, emailSender);
    }

    @Test
    @DisplayName("hands back whatever the gateway said")
    void returnsTheGatewayResult() {
        when(gateway.charge(order)).thenReturn(new Approved("TX-1"));

        PaymentResult result = service.place(order);

        assertThat(result).isEqualTo(new Approved("TX-1"));
    }

    @Test
    @DisplayName("saves the order when the payment is approved")
    void savesOnApproval() {
        when(gateway.charge(order)).thenReturn(new Approved("TX-1"));

        service.place(order);

        verify(repository).save(order);
    }

    @Test
    @DisplayName("emails the customer, with the transaction id from the gateway")
    void emailsOnApproval() {
        when(gateway.charge(order)).thenReturn(new Approved("TX-1"));

        service.place(order);

        verify(emailSender).sendConfirmation(order, "TX-1");
    }

    @Test
    @DisplayName("saves nothing and emails nobody when the payment is declined")
    void doesNothingOnDecline() {
        when(gateway.charge(order)).thenReturn(new Declined("card expired"));

        service.place(order);

        verify(repository, never()).save(order);
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("saves nothing and emails nobody when the item is out of stock")
    void doesNothingWhenOutOfStock() {
        when(gateway.charge(order)).thenReturn(new OutOfStock(0));

        service.place(order);

        verify(repository, never()).save(order);
        verifyNoInteractions(emailSender);
    }
}
