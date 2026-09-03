import java.util.ArrayList;
import java.util.List;

/**
 * PART 1 — SOLUTION.
 *
 * Note the fourth component. `tags` is a List, and a List is mutable, so
 * `final` on the field is not enough: List.copyOf in the compact constructor
 * is what actually closes the door. This is the trap from
 * ../examples/RecordAnatomy.java, section 6.
 */
public record Ticket(String id,
                     String requesterEmail,
                     String summary,
                     List<String> tags) {

    public Ticket {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new IllegalArgumentException("requesterEmail is required");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary is required");
        }
        id = id.trim().toUpperCase();
        requesterEmail = requesterEmail.trim().toLowerCase();
        summary = summary.trim();
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** A wither: a NEW Ticket with one more tag. The original is untouched. */
    public Ticket withExtraTag(String tag) {
        var more = new ArrayList<>(tags);
        more.add(tag);
        return new Ticket(id, requesterEmail, summary, more);
    }
}
