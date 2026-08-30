# Demo programs

Four small programs your instructor runs on screen during the session. **They are not your
work** — your work is the exercise in `../src/` and the homework in `../homework/`. Nothing here
is graded and nothing here is checked by `Verify`.

They are here so you can replay them afterwards, at your own pace, when a point from the session
has gone fuzzy. No build tool needed:

```
java VarLab.java
java RecordAnatomy.java
java SealedProof.java
java TextBlockLab.java
```

One file, one command, no compile step — Java runs a single source file directly. Edit in
IntelliJ if you like, but run here: with no build tool in the folder, IntelliJ cannot reliably
work out which class to launch. Its built-in terminal (**Alt+F12**) keeps you in one window.

## They all run the same way

Each one prints a short **BEFORE** block, then an **AFTER** block — the same lines, with the
modern feature in place. The source in between shows what you change to get from one to the
other. Nothing prints more than a dozen lines.

| File | BEFORE | AFTER |
|---|---|---|
| `VarLab.java` | three variables with the type written out | the same three with `var` — identical types, and one gotcha |
| `RecordAnatomy.java` | `toString` is `OldOrder@76ed…`, `equals` false, a `HashSet` that loses one | the same three lines from a record, all correct — then the compact constructor and the trap |
| `SealedProof.java` | two status-blob states that compile and mean nothing | three records that cannot express either |
| `TextBlockLab.java` | SQL glued with `+`, printing `FROM product pJOIN category c` | the same strings as text blocks — then the closing-`"""` rule |

## For the instructor

Each file ends in a short **LIVE DEMO** block: the lines to type on screen and what to read out.
The `SealedProof` one is the high point of the session — a fourth outcome added without
permission, and the compiler refusing it by name. Do not skip it, and read the message verbatim.

Undo the live edits before the next run; every block says so.

## If you are replaying one on your own

Read the BEFORE block, cover the terminal, and say what you expect AFTER to print. Then run it.
