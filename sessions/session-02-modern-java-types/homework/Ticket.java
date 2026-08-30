import java.util.List;

/**
 * PART 1 — Replace this class with a record.
 *
 * A support ticket. Four things: an id, who raised it, what it says, and any
 * tags on it.
 *
 * TODO: delete everything below and write, in its place:
 *
 *   public record Ticket(String id,
 *                        String requesterEmail,
 *                        String summary,
 *                        List<String> tags) { ... }
 *
 * Then a COMPACT CONSTRUCTOR that:
 *
 *   1. rejects a null or blank id      -> IllegalArgumentException("id is required")
 *   2. rejects a null or blank email   -> IllegalArgumentException("requesterEmail is required")
 *   3. rejects a null or blank summary -> IllegalArgumentException("summary is required")
 *   4. normalises: id trimmed and UPPER-cased, email trimmed and lower-cased,
 *      summary trimmed
 *   5. handles the tags list — see the warning below
 *
 * And one method:
 *
 *   public Ticket withExtraTag(String tag)
 *       // returns a NEW Ticket with one more tag; the original is untouched
 *
 * ---------------------------------------------------------------------------
 *  THE PART THAT IS NOT LIKE THE EXERCISE — READ IT BEFORE YOU START
 * ---------------------------------------------------------------------------
 *
 *  `tags` is a List, and a List is MUTABLE. Making the record does not protect
 *  it. `final` stops the field being re-pointed at a different list; it says
 *  nothing about the list it points at. So this is possible:
 *
 *      var mine = new ArrayList<String>();
 *      mine.add("account");
 *      var ticket = new Ticket("SUP-1", "a@b.com", "S", mine);
 *      mine.add("SNEAKED-IN");        // the caller still holds the list...
 *      ticket.tags();                 // ...and the ticket now has two tags
 *
 *  Two checks in Verify try exactly that. Close the hole in the compact
 *  constructor, in one line:
 *
 *      tags = tags == null ? List.of() : List.copyOf(tags);
 *
 *  List.copyOf does two jobs at once: it copies (so the caller's later edits
 *  cannot reach in) and the copy is unmodifiable (so nobody who gets the list
 *  back from tags() can edit it either). Note the null case — Verify expects
 *  a null tags list to become an EMPTY list, not to blow up.
 *
 *  ../examples/RecordAnatomy.java, section 6, demonstrates the whole problem.
 */
public class Ticket {

    private final String id;
    private final String requesterEmail;
    private final String summary;
    private final List<String> tags;

    public Ticket(String id, String requesterEmail, String summary, List<String> tags) {
        this.id = id;
        this.requesterEmail = requesterEmail;
        this.summary = summary;
        this.tags = tags;
    }

    public String id()             { return id; }
    public String requesterEmail() { return requesterEmail; }
    public String summary()        { return summary; }
    public List<String> tags()     { return tags; }
}
