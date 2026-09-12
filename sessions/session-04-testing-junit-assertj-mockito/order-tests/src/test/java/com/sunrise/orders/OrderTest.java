package com.sunrise.orders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * REFERENCE SOLUTION — 7 class tests (2 given + 5 written) and 2 homework tests.
 */
@DisplayName("Order")
class OrderTest {

    @Nested
    @DisplayName("when it is built")
    class Construction {

        @Test
        @DisplayName("keeps the values it was given")
        void keepsItsValues() {
            Order order = new Order("chan@example.com", 250.00, 2);

            assertThat(order.customerEmail()).isEqualTo("chan@example.com");
            assertThat(order.total()).isEqualTo(250.00, within(0.001));
            assertThat(order.quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("trims and lower-cases the email")
        void normalisesTheEmail() {
            Order order = new Order("  CHAN@Example.COM  ", 10.00, 1);

            assertThat(order.customerEmail()).isEqualTo("chan@example.com");
        }

        // HOMEWORK 3 — pins down what is ALLOWED. Goes red if `total < 0` becomes `total <= 0`.
        @Test
        @DisplayName("accepts an order that is legitimately free")
        void acceptsAFreeOrder() {
            Order order = new Order("chan@example.com", 0.00, 1);

            assertThat(order.total()).isEqualTo(0.00, within(0.001));
        }
    }

    @Nested
    @DisplayName("when the arguments are wrong")
    class Validation {

        @Test
        @DisplayName("rejects an email with no @")
        void rejectsEmailWithoutAt() {
            assertThatThrownBy(() -> new Order("not-an-email", 10.00, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customerEmail");
        }

        @Test
        @DisplayName("rejects a negative total")
        void rejectsNegativeTotal() {
            assertThatThrownBy(() -> new Order("chan@example.com", -0.01, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("total");
        }

        @Test
        @DisplayName("rejects a quantity below one")
        void rejectsZeroQuantity() {
            assertThatThrownBy(() -> new Order("chan@example.com", 10.00, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity");
        }
    }

    @Nested
    @DisplayName("withDiscount")
    class Discount {

        @Test
        @DisplayName("takes the percentage off the total")
        void takesThePercentageOff() {
            Order order = new Order("chan@example.com", 200.00, 1);

            Order discounted = order.withDiscount(10);

            assertThat(discounted.total()).isEqualTo(180.00, within(0.001));
        }

        @Test
        @DisplayName("leaves the original untouched")
        void leavesTheOriginalAlone() {
            Order order = new Order("chan@example.com", 200.00, 1);

            order.withDiscount(10);

            assertThat(order.total()).isEqualTo(200.00, within(0.001));
        }

        // HOMEWORK 1 — a negative percentage, because withDiscount(150) still throws with the
        // guard deleted: the constructor refuses the negative total it produces.
        @Test
        @DisplayName("rejects a percentage outside 0 to 100")
        void rejectsAnInvalidPercentage() {
            Order order = new Order("chan@example.com", 200.00, 1);

            assertThatThrownBy(() -> order.withDiscount(-10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("percent");
        }
    }
}
