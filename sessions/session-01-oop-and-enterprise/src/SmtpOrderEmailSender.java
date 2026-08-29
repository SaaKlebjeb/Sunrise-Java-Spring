/**
 * GIVEN — nothing to do here.
 *
 * One way to keep the EmailSender promise.
 */
public class SmtpOrderEmailSender implements EmailSender {

    @Override
    public void send(String to, String message) {
        System.out.printf("[SMTP] To: %s | %s%n", to, message);
    }
}
