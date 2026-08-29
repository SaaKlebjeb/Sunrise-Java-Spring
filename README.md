# Java Enterprise Spring Framework — Core

Modern Java → the Spring container → REST APIs → shipped in a container.

> **Session 1 is available now.** The rest of the course is being written and will land here
> session by session. Everything below describes Session 1.

---

## Before you start

- **JDK 25.** Open a terminal and run:

  ```
  java -version
  javac -version
  ```

  Both must print 25. If `javac` is missing you have a JRE, not a JDK — install the JDK.

- **IntelliJ IDEA** (Community Edition is fine). This is the editor the course assumes.
  You will **edit** here and **run** from a terminal — see [How you run things](#how-you-run-things).

- **Comfort with Java classes, interfaces and collections.** If any of that is shaky, work
  through [`sunrise-java-fundermental`](../sunrise-java-fundermental) sessions 1–5 first.

Nothing else. No Maven, no Docker, no accounts — Session 1 is plain Java on purpose.

---

## What's in here

```
sessions/session-01-oop-and-enterprise/
  ├── lecture.md      ← start here. Read it top to bottom
  ├── examples/       ← two small programs you RUN while reading
  └── src/            ← the exercise, plus the checker you run yourself
```

| Path | What it is |
|---|---|
| `sessions/session-01-oop-and-enterprise/lecture.md` | The guided lesson. This is the main text |
| `.../examples/` | `BlastRadius.java` and `DispatchDemo.java` — run them, don't just read them |
| `.../src/` | The exercise. Almost everything is written for you; you make four small edits |
| `.../src/Verify.java` | The checker. 8 checks, PASS/FAIL each. **Don't edit it** |
| `slides/` | The slide deck, if you want to review what was on screen |

---

## How you run things

**You edit in IntelliJ. You run from a terminal.**

Session 1 is a folder of plain `.java` files with no build tool. In that arrangement IntelliJ
cannot reliably work out which class to launch, so its green run arrows either aren't there or
start the wrong thing. Once Maven arrives later in the course, the run buttons work properly
and you get them back.

You don't have to leave IntelliJ to do this — it has a terminal built in:
**View → Tool Windows → Terminal**, or **Alt+F12**.

| What you want to run | Command |
|---|---|
| Either example program | `java BlastRadius.java` — from `examples/` |
| The exercise checker | `javac *.java` then `java Verify` — from `src/` |
| The exercise application | `java Main` — from `src/`, after compiling |

A single `.java` file runs directly, with no compile step. A *folder* of them needs `javac` first.

---

## Working through Session 1

**1. Read `lecture.md` from the top.**

It's a guided lesson, not a reference — read it in order. Stop at every **Pause** prompt and
actually answer it before reading on. Those are the parts that stick.

**2. Run the examples when the lecture tells you to.**

Two `Run it.` callouts appear at the right moments in the text. Don't run them early and don't
skip them:

- **`BlastRadius.java`** — two orders that behave identically until one is fed nonsense. Then
  the comment at the bottom asks you to change one field's type and **count the compile
  errors**. That count is the point of the whole exercise.
- **`DispatchDemo.java`** — **predict all four answers on paper before you run it.** Three of
  them catch almost everybody. Getting a prediction wrong on paper is how you remember it.

**3. Do the exercise in `src/`.**

This is a fill-in, not a build-from-scratch. `Order`, both interfaces and all three
implementations are **already written and working** — read them, don't change them. Your job is
**four small edits across two files**, and every file tells you exactly what to do in a comment
at the top.

**4. Check your own work.**

From `src/`:

```
javac *.java
java Verify
```

Both commands, after every edit — press the up-arrow twice in the terminal and you're
re-running in two keystrokes.

There are **8 checks**, PASS or FAIL each. When all 8 pass, you're done. There is no "ask the
instructor whether it's right" step — the checker tells you.

**5. If you finish early**, the lecture has three optional extensions near the end
(*Finished early? Three extensions*). They aren't
checked by `Verify` and they're worth doing — the second one especially, because it's a trick
Spring automates later and it's much better to have done it by hand first.

---

## If something goes wrong

**`java Verify` says the constructor is missing, and nothing compiles.**
That's expected before you've done the second edit — `Verify` needs your constructor to exist.
One clean error is what a correct starting point looks like. Do the first two edits, then run again.

**Your shell doesn't understand `javac *.java`.**
Some Windows shells don't expand the `*`. List the files instead — the lecture spells out the
full command.

**Some other compile error.**
The lecture has an **If you get stuck** table covering the four errors students actually hit,
and what each one means. Check there before asking.

**`javac: command not found`.**
You have a JRE, not a JDK. See [Before you start](#before-you-start).

---

## Slides

The deck for the session, if you want to review it:

```bash
pnpm install
pnpm run dev
```

Then open <http://localhost:3030>. Built with [Slidev](https://sli.dev/).

---

## Versions

**Java 25.** Later sessions add **Spring Boot 4.0.3** and **Maven**. Don't mix versions.

