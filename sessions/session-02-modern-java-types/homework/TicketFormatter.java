/**
 * PARTS 3, 4 and 5 — SOLUTION.
 */
public class TicketFormatter {

    /** PART 3 — instanceof with pattern variables. No casts. */
    public String describe(TicketOutcome outcome) {
        if (outcome instanceof Resolved r) {
            return "Resolved by " + r.agent() + " in " + r.minutesTaken() + " min";
        }
        if (outcome instanceof Escalated e) {
            return "Escalated to " + e.team() + ": " + e.reason();
        }
        if (outcome instanceof Rejected x) {
            return "Rejected: " + x.reason();
        }
        throw new IllegalStateException("unknown outcome: " + outcome);
    }

    /**
     * PART 4 — a text block that ENDS WITH a newline.
     * Closing """ on its own line, aligned with the content.
     */
    public String card(Ticket ticket, TicketOutcome outcome) {
        return """
                TICKET %s
                Requester: %s
                Summary:   %s
                Tags:      %s
                Outcome:   %s
                """.formatted(ticket.id(),
                              ticket.requesterEmail(),
                              ticket.summary(),
                              ticket.tags(),
                              describe(outcome));
    }

    /**
     * PART 5 — a text block that does NOT end with a newline.
     * Closing """ tucked onto the end of the last content line.
     *
     * The SQL reads like SQL, which is the whole point. Compare it with the
     * five-way string concatenation this replaced.
     */
    public String auditQuery(Ticket ticket) {
        return """
                SELECT t.id, t.requester_email, t.status
                FROM ticket t
                WHERE t.id = '%s'
                ORDER BY t.created_at DESC""".formatted(ticket.id());
    }
}
