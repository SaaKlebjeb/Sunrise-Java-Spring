/**
 * GIVEN — nothing to do here.
 *
 * The capability: "notify somebody."
 *
 * No mention of SMTP, ports or TLS. Same reason as OrderRepository.
 */
public interface EmailSender {

    void send(String to, String message);
}
