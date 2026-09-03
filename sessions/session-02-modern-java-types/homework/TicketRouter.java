/**
 * PART 2 — SOLUTION. Deterministic rules, applied in order.
 */
public class TicketRouter {

    public TicketOutcome route(Ticket ticket) {
        if (ticket.summary().toLowerCase().contains("password")) {
            return new Resolved("bot", 2);
        }
        if (ticket.tags().contains("security")) {
            return new Escalated("security-team", "tagged security");
        }
        if (ticket.requesterEmail().endsWith("@spam.example")) {
            return new Rejected("Unknown requester domain");
        }
        return new Escalated("tier-2", "needs a human");
    }
}
