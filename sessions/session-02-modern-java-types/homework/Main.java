import java.util.List;

/**
 * PART 6 — run it. GIVEN, complete, nothing to fill in.
 *
 * Expected output:
 *
 *   TICKET SUP-1001
 *   Requester: alice@example.com
 *   Summary:   Cannot reset password
 *   Tags:      [account]
 *   Outcome:   Resolved by bot in 2 min
 *
 *   TICKET SUP-1002
 *   Requester: bob@example.com
 *   Summary:   Suspicious login from another country
 *   Tags:      [security, urgent]
 *   Outcome:   Escalated to security-team: tagged security
 *
 *   TICKET SUP-1003
 *   Requester: nobody@spam.example
 *   Summary:   YOU HAVE WON
 *   Tags:      []
 *   Outcome:   Rejected: Unknown requester domain
 *
 *   TICKET SUP-1004
 *   Requester: carol@example.com
 *   Summary:   Printer makes a grinding noise
 *   Tags:      [hardware]
 *   Outcome:   Escalated to tier-2: needs a human
 *
 *   --- audit query for the first ticket ---
 *   SELECT t.id, t.requester_email, t.status
 *   FROM ticket t
 *   WHERE t.id = 'SUP-1001'
 *   ORDER BY t.created_at DESC
 *
 *   --- immutability ---
 *   original:  Ticket[id=SUP-1001, requesterEmail=alice@example.com, summary=Cannot reset password, tags=[account]]
 *   with tag:  Ticket[id=SUP-1001, requesterEmail=alice@example.com, summary=Cannot reset password, tags=[account, vip]]
 *   original:  Ticket[id=SUP-1001, requesterEmail=alice@example.com, summary=Cannot reset password, tags=[account]]
 *   Same value? true
 */
public class Main {

    public static void main(String[] args) {

        var router = new TicketRouter();
        var formatter = new TicketFormatter();

        var tickets = new Ticket[] {
                // note: messy id and email — the compact constructor cleans them
                new Ticket(" sup-1001 ", "Alice@Example.com ",
                        "Cannot reset password", List.of("account")),
                new Ticket("SUP-1002", "bob@example.com",
                        "Suspicious login from another country", List.of("security", "urgent")),
                new Ticket("SUP-1003", "nobody@spam.example",
                        "YOU HAVE WON", List.of()),
                new Ticket("SUP-1004", "carol@example.com",
                        "Printer makes a grinding noise", List.of("hardware"))
        };

        for (var ticket : tickets) {
            System.out.println(formatter.card(ticket, router.route(ticket)));
        }

        System.out.println("--- audit query for the first ticket ---");
        System.out.println(formatter.auditQuery(tickets[0]));
        System.out.println();

        System.out.println("--- immutability ---");
        var original = tickets[0];
        var tagged = original.withExtraTag("vip");
        System.out.println("original:  " + original);
        System.out.println("with tag:  " + tagged);
        System.out.println("original:  " + original);

        var a = new Ticket("SUP-9", "z@example.com", "Same", List.of("x"));
        var b = new Ticket("SUP-9", "z@example.com", "Same", List.of("x"));
        System.out.println("Same value? " + a.equals(b));
    }
}
