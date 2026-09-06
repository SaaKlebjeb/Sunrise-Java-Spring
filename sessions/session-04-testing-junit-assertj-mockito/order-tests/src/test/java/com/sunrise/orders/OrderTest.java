package com.sunrise.orders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ============================================================
 * PART ONE — testing a value type
 * ============================================================
 * Two tests are written for you. They are the two shapes you
 * need for everything else in this file:
 *
 *   - assertThat(...)          for "it produced this"
 *   - assertThatThrownBy(...)  for "it refused"
 *
 * Write the five marked TODO. Each one currently calls
 * fail(...), so the suite is RED until you write it — delete
 * that line as you go. Keep the Arrange / Act / Assert shape
 * and the blank lines between them — a test you can read in
 * three seconds is worth more than a clever one.
 * ============================================================
 */
@DisplayName("Order")
class OrderTest {

    @Nested
    @DisplayName("when it is built")
    class Construction {

        // ---- GIVEN, as a model. Do not change it. ----
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
            // TODO: build an Order with "  CHAN@Example.COM  " as the email
            //       and assert that customerEmail() comes back "chan@example.com".
            fail("TODO: write this test");
        }
    }

    @Nested
    @DisplayName("when the arguments are wrong")
    class Validation {

        // ---- GIVEN, as a model. Do not change it. ----
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
            // TODO: same shape as above. Assert the message mentions "total".
            fail("TODO: write this test");
        }

        @Test
        @DisplayName("rejects a quantity below one")
        void rejectsZeroQuantity() {
            // TODO: same shape again.
            fail("TODO: write this test");
        }
    }

    @Nested
    @DisplayName("withDiscount")
    class Discount {

        @Test
        @DisplayName("takes the percentage off the total")
        void takesThePercentageOff() {
            // TODO: 10% off an order of 200.00 is 180.00.
            //       Compare doubles with within(0.001), never with ==.
            fail("TODO: write this test");
        }

        @Test
        @DisplayName("leaves the original untouched")
        void leavesTheOriginalAlone() {
            // TODO: call withDiscount and then assert the ORIGINAL order's
            //       total is still what it was. This is the test that proves
            //       the record is immutable rather than assuming it.
            fail("TODO: write this test");
        }
    }
}
