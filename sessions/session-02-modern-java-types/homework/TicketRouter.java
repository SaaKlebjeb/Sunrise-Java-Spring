/**
 * PART 2 — the routing rules.
 */
public class TicketRouter {

    /**
     * TODO: apply these rules IN THIS ORDER and return the matching record.
     *
     *   1. the summary contains "password" (case-insensitive)
     *        -> new Resolved("bot", 2)
     *
     *   2. the tags contain "security"
     *        -> new Escalated("security-team", "tagged security")
     *
     *   3. the requester email ends with "@spam.example"
     *        -> new Rejected("Unknown requester domain")
     *
     *   4. otherwise
     *        -> new Escalated("tier-2", "needs a human")
     *
     * The ORDER matters, and Verify checks it: a ticket that is both a
     * password ticket AND tagged security must come back Resolved, because
     * rule 1 is tested first.
     *
     * Useful: ticket.summary().toLowerCase().contains("password")
     *         ticket.tags().contains("security")
     *         ticket.requesterEmail().endsWith("@spam.example")
     */
    public TicketOutcome route(Ticket ticket) {
        return null; // replace this
    }
}
