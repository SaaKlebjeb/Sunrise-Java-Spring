package com.sunrise.orders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ============================================================
 * PART THREE — testing collaboration
 * ============================================================
 * `OrderService` calls three other things. Its whole job IS
 * calling them, in the right order, under the right condition.
 * So `assertThat(result)` cannot test most of it.
 *
 * That is what `verify` is for: asserting on a call that
 * happened, rather than on a value that came back.
 *
 * The three mocks and one test are written for you.
 * Write the four marked TODO — each one calls fail(...) until
 * you do. Delete that line as you write the test.
 * ============================================================
 */
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

    // ---- GIVEN, as a model. Do not change it. ----
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
        // TODO: stub an Approved result, place the order,
        //       then verify(repository).save(order).
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("emails the customer, with the transaction id from the gateway")
    void emailsOnApproval() {
        // TODO: the transaction id in the email must be the one the GATEWAY
        //       returned, not one the test invented twice. Stub "TX-1" and
        //       verify the confirmation was sent with "TX-1".
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("saves nothing and emails nobody when the payment is declined")
    void doesNothingOnDecline() {
        // TODO: stub a Declined result. Then assert two negatives:
        //       verify(repository, never()).save(order);
        //       verifyNoInteractions(emailSender);
        fail("TODO: write this test");
    }

    @Test
    @DisplayName("saves nothing and emails nobody when the item is out of stock")
    void doesNothingWhenOutOfStock() {
        // TODO: the same, for OutOfStock. Two outcomes, one behaviour —
        //       and if someone later adds a fourth outcome, neither of
        //       these tests will notice. Worth remembering.
        fail("TODO: write this test");
    }
}
