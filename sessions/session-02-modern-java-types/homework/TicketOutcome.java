/**
 * PART 2 — A closed set of outcomes.
 *
 * A ticket ends exactly three ways: someone resolves it, it goes to another
 * team, or it is not a real ticket at all.
 *
 * TODO: make this a sealed interface permitting exactly Resolved, Escalated
 *       and Rejected:
 *
 *   public sealed interface TicketOutcome
 *           permits Resolved, Escalated, Rejected { }
 */
public interface TicketOutcome {
}
