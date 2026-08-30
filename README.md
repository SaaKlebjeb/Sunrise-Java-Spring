# Java Enterprise Spring Framework — Core

Modern Java → the Spring container → REST APIs → shipped in a container.

> **Sessions 1 and 2 are available now.** The rest of the course is being written and will land
> here session by session.

---

## Before you start

- **JDK 25.** Open a terminal and run:

  ```
  java -version
  javac -version
  ```

  Both must print 25. If `javac` is missing you have a JRE, not a JDK — install the JDK.

  This matters more from Session 2 on: **records need JDK 16+ and sealed types need 17+.**
  On an old JDK, half of Session 2 will not compile.

- **IntelliJ IDEA** (Community Edition is fine). This is the editor the course assumes.
  You will **edit** here and **run** from a terminal — see [How you run things](#how-you-run-things).

- **Comfort with Java classes, interfaces and collections.** If any of that is shaky, work
  through [`sunrise-java-fundermental`](../sunrise-java-fundermental) sessions 1–5 first.

Nothing else. No Maven, no Docker, no accounts — these sessions are plain Java on purpose.

---

## What's in here

```
sessions/session-01-oop-and-enterprise/
  ├── lecture.md      ← start here. Read it top to bottom
  ├── examples/       ← two small programs you RUN while reading
  └── src/            ← the exercise, plus the checker you run yourself

sessions/session-02-modern-java-types/
  ├── lecture.md      ← the guided lesson
  ├── examples/       ← four demo programs, run in class — replay them any time
  ├── src/            ← the in-class exercise + its checker
  └── homework/       ← a second exercise, for afterwards, with its own checker
```

| Path | What it is |
|---|---|
| `sessions/session-XX-…/lecture.md` | The guided lesson. This is the main text |
| `.../examples/` | Small programs demonstrated in class. Yours to replay, never graded |
| `.../src/` | The exercise. Almost everything is written for you |
| `.../src/Verify.java` | The checker. PASS/FAIL per check. **Don't edit it** |
| `.../homework/` | *(Session 2 on)* A second exercise in its own domain, with its own `Verify` |
| `slides/` | The slide decks, if you want to review what was on screen |

---

## The sessions so far

| Session | Topic | You run | Checks |
|---|---|---|---|
| [1](sessions/session-01-oop-and-enterprise/lecture.md) | **Java OOP & the Enterprise Context** — the four pillars honestly, program to an interface, constructor injection, Jakarta EE and where Spring sits | `BlastRadius`, `DispatchDemo` | 8 |
| [2](sessions/session-02-modern-java-types/lecture.md) | **Modern Java Types** — `var`, records, compact constructors, sealed types, text blocks | `VarLab`, `RecordAnatomy`, `SealedProof`, `TextBlockLab` | 24 + **26 homework** |

---

## How you run things

**You edit in IntelliJ. You run from a terminal.**

These sessions are folders of plain `.java` files with no build tool. In that arrangement
IntelliJ cannot reliably work out which class to launch, so its green run arrows either aren't
there or start the wrong thing. Once Maven arrives later in the course, the run buttons work
properly and you get them back.

You don't have to leave IntelliJ to do this — it has a terminal built in:
**View → Tool Windows → Terminal**, or **Alt+F12**.

| What you want to run | Command |
|---|---|
| Any demo program (to replay) | `java BlastRadius.java`, `java RecordAnatomy.java`, … — from `examples/` |
| The exercise checker | `javac *.java` then `java Verify` — from `src/` |
| The exercise application | `java Main` — from `src/`, after compiling |
| The homework checker | `javac *.java` then `java Verify` — from `homework/` |

A single `.java` file runs directly, with no compile step. A *folder* of them needs `javac` first.

---

## Working through a session

**1. Read `lecture.md` from the top.**

It's a guided lesson, not a reference — read it in order. Stop at every **Pause** prompt and
actually answer it before reading on. Those are the parts that stick.

**2. Watch the demos, and replay them afterwards.**

A `Demo` callout appears at the right moment in the text. Those programs are run on screen
during the session — you don't need to run them yourself while reading. They stay in
`examples/` so you can replay any of them later, when a point has gone fuzzy. Predict the
output before you look.

Session 1: `BlastRadius.java` (count the compile errors — that count is the blast radius) and
`DispatchDemo.java` (predict all four answers on paper first).

Session 2: `VarLab.java`, `RecordAnatomy.java`, `SealedProof.java` and `TextBlockLab.java`.
Each one runs *the ordinary way → the problem → the refactor*; see
`sessions/session-02-modern-java-types/examples/README.md` for what each one shows.

**3. Do the exercise in `src/`.**

These are fill-ins, not build-from-scratch. Given files are marked, and every file you *do*
touch tells you exactly what to write in a comment at the top.

**4. Check your own work.**

From the exercise folder:

```
javac *.java
java Verify
```

Both commands, after every edit — press the up-arrow twice in the terminal and you're
re-running in two keystrokes.

Each check is PASS or FAIL. When all of them pass, you're done. There is no "ask the instructor
whether it's right" step — the checker tells you.

**5. If you finish early**, each lecture ends with optional extensions. They aren't checked by
`Verify` and they're worth doing.

**6. Session 2 onward, do the homework.** It lives in `homework/`, has its own domain and its
own `Verify`, and it is not a continuation — if the in-class exercise went badly you can still
do it cleanly. Read `homework/README.md` first.

---

## If something goes wrong

**Nothing compiles when you first open the exercise.**
That is expected in both sessions, and the lecture says exactly how many errors to expect and
why. Session 1: one error, about a missing constructor. Session 2: nine errors, from two
causes. Do the first steps, then compile.

**Your shell doesn't understand `javac *.java`.**
Some Windows shells don't expand the `*`. List the files instead — the lecture spells out the
full command.

**A text-block check fails and the two strings look identical.** *(Session 2)*
It's whitespace. `Verify` says so explicitly when that's the only difference. Count the leading
spaces — your closing `"""` is in the wrong column — then count the `\n` at the end.

**Some other compile error.**
Each lecture has an **If you get stuck** table covering the errors students actually hit, and
what each one means. Check there before asking.

**`javac: command not found`.**
You have a JRE, not a JDK. See [Before you start](#before-you-start).

---

## Slides

The decks, if you want to review them:

```bash
pnpm install
pnpm run dev
```

Then open <http://localhost:3030>. Built with [Slidev](https://sli.dev/).

`slides.md` shows **one session at a time** — the instructor uncomments the session being
taught. If you want a different one, swap which `src:` line is uncommented.

---

## Versions

**Java 25.** Later sessions add **Spring Boot 4.0.3** and **Maven**. Don't mix versions.
