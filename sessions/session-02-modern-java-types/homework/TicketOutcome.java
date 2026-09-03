/**
 * PART 2 — SOLUTION.
 *
 * "A TicketOutcome is a Resolved, an Escalated, or a Rejected —
 *  and nothing else, ever."
 */
public sealed interface TicketOutcome
        permits Resolved, Escalated, Rejected { }
