/**
 * PARTS 3, 4 and 5 — turning a ticket and an outcome into text.
 */
public class TicketFormatter {

    /**
     * PART 3 — describe an outcome, with `instanceof` and pattern variables.
     * No casts anywhere in this method.
     *
     * Expected results:
     *   Resolved("bot", 2)                            -> "Resolved by bot in 2 min"
     *   Escalated("security-team", "tagged security") -> "Escalated to security-team: tagged security"
     *   Rejected("Unknown requester domain")          -> "Rejected: Unknown requester domain"
     *
     * You will need a final `throw` after the three ifs or the method will not
     * compile. It is unreachable, and it is temporary — next session one
     * exhaustive switch replaces this whole body and the dead line goes away.
     */
    public String describe(TicketOutcome outcome) {
        return null; // replace this
    }

    /**
     * PART 4 — the ticket card, as a TEXT BLOCK. This one ENDS WITH a newline.
     *
     * Target output (exactly — mind the column alignment):
     *
     *   TICKET SUP-1001
     *   Requester: alice@example.com
     *   Summary:   Cannot reset password
     *   Tags:      [account]
     *   Outcome:   Resolved by bot in 2 min
     *
     * Five %s holes, filled with .formatted(...). The Tags line is just the
     * list — `%s` on a List gives you [account] for free.
     *
     * Keep the closing \"\"\" on its own line, in the same column as the
     * content: that is what gives you the trailing newline and strips the
     * indentation.
     *
     * Call describe(outcome) for the last line. Do not hard-code it.
     */
    public String card(Ticket ticket, TicketOutcome outcome) {
        return null; // replace this
    }

    /**
     * PART 5 — an audit query, as a TEXT BLOCK. This one does NOT end with a
     * newline.
     *
     * Target output (exactly — the last character is the C of DESC):
     *
     *   SELECT t.id, t.requester_email, t.status
     *   FROM ticket t
     *   WHERE t.id = 'SUP-1001'
     *   ORDER BY t.created_at DESC
     *
     * This is the other thing text blocks are for. Written the old way it is
     * four string literals, four trailing spaces you have to remember, and
     * four plus signs:
     *
     *   "SELECT t.id, t.requester_email, t.status " +
     *   "FROM ticket t " +
     *   "WHERE t.id = '" + ticket.id() + "' " +
     *   "ORDER BY t.created_at DESC"
     *
     * Written as a text block it is SQL that looks like SQL, and a reviewer
     * can read it.
     *
     * (Building SQL by pasting a value straight into the string is fine here
     *  because nothing is talking to a database. In real code that is an SQL
     *  injection, and you use a parameter — `WHERE t.id = ?` — instead. The
     *  persistence course covers it properly.)
     */
    public String auditQuery(Ticket ticket) {
        return null; // replace this
    }
}
