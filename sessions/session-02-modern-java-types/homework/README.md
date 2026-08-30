# Homework — The support desk

**Do this on your own, after the session. Budget 45–60 minutes.**

Same four features as the in-class exercise, different domain, and one thing the exercise
never tested. It does **not** continue the exercise: if the order-processing exercise went
badly, you can still do this one cleanly.

## How you check your work

From a terminal in this folder:

```
javac *.java
java Verify
```

Both commands, after every edit — up-arrow twice to repeat them.

**26 checks.** `ALL 26 CHECKS PASSED` means you are finished. There is no "ask the instructor
whether it's right" step.

The unedited folder does **not** compile — you get **7 errors**, all from two causes:
`Ticket` has no `withExtraTag` yet, and the three outcome types have no constructors yet
because they are still empty classes. That is expected. Do Part 1 and Part 2, then compile.

## The domain

A support desk. A `Ticket` comes in; a `TicketRouter` decides what happens to it; a
`TicketFormatter` turns the result into something a human or a log can read.

## What to do

| Part | File(s) | What |
|---|---|---|
| 1 | `Ticket.java` | Make it a record with four components. Compact constructor: validate, normalise, **and defend the tags list**. Add `withExtraTag`. |
| 2 | `TicketOutcome.java`, `Resolved.java`, `Escalated.java`, `Rejected.java`, `TicketRouter.java` | Seal the outcome over three records. Implement the four routing rules, **in order**. |
| 3 | `TicketFormatter.describe` | `instanceof` with pattern variables. No casts. |
| 4 | `TicketFormatter.card` | A text block that **ends with** a newline. |
| 5 | `TicketFormatter.auditQuery` | A text block that **does not**. SQL this time, not JSON. |
| 6 | `Main.java` | Given and complete. Run it and read the output. |

`Main.java` and `Verify.java` are **given** — do not edit them. Every other file has `TODO:`
comments telling you exactly what to write.

## The one part that is not like the exercise

`Ticket` holds a `List<String> tags`, and **a list is mutable**. Making `Ticket` a record does
not protect it: `final` stops the field being re-pointed at a *different* list, and says
nothing about the list it already points at. So without a defensive copy this works:

```java
var mine = new ArrayList<String>();
mine.add("account");
var ticket = new Ticket("SUP-1", "a@b.com", "S", mine);
mine.add("SNEAKED-IN");        // the caller still holds the list
ticket.tags();                 // [account, SNEAKED-IN]
```

Two checks try exactly that. `List.copyOf(tags)` in the compact constructor closes both holes
at once — it copies, *and* the copy refuses `add()`. Watch the null case: a null tags list must
become an **empty** list, not an exception.

`../examples/RecordAnatomy.java`, section 6, demonstrates the whole problem and its fix.

## If you get stuck

| The compiler or `Verify` says | What it means |
|---|---|
| `cannot find symbol: method withExtraTag` | Part 1 not done. Nothing compiles until it is. |
| `constructor Resolved ... cannot be applied to given types` | Part 2: `Resolved` is still an empty class, not a record. |
| `class is not allowed to extend sealed class` | You sealed `TicketOutcome` but left a type out of `permits`. |
| `the list returned by tags() refuses add()` fails | You stored the caller's list directly. Use `List.copyOf`. |
| `editing the caller's list does not change the ticket` fails | Same cause as the row above. |
| A text-block check fails and the strings look identical | Count the leading spaces, and count the `\n` at the end. `Verify` tells you when only the whitespace differs. |
| `the rules are applied in order` fails | You used `else if` in the wrong order, or checked tags before the summary. |

## Finished early? Two extensions

Neither is checked by `Verify` — the count stays at 26.

**A · A fourth outcome.** Add `record Duplicate(String originalTicketId) implements
TicketOutcome`. First try it *without* touching `permits` and read the error. Then add it
properly, and route any summary starting with `"RE:"` to it.

Now the important bit: **`describe()` still compiles**, and it will throw at runtime for a
`Duplicate`. Nothing warned you. Write down what you would want the compiler to have done. That
is precisely what next session's exhaustive `switch` gives you.

**B · Argue for the blob.** Write the version of this you would have written last week: one
`TicketResult` record with `String status`, `String agent`, `Integer minutes`, `String team`,
`String reason`. Then list three field combinations it allows that mean nothing. Keep the list
— it is the honest answer to a colleague who asks why three types are better than one.
