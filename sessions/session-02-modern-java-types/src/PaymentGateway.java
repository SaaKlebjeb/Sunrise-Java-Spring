/** Given. Name the capability, not the mechanism. */
public interface PaymentGateway {

    PaymentResult charge(Order order);
}
