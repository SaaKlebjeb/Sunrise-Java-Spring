# Java Enterprise Spring Framework — Core

Modern Java → the Spring container → REST APIs → shipped in a container.

> **Sessions 1 to 4 are available now.** The rest of the course is being written and will land
> here session by session.

---

## Before you start

- **JDK 25.** Open a terminal and run:

  ```
  java -version
  javac -version
  ```

  Both must print 25. If `javac` is missing you have a JRE, not a JDK — install the JDK.

  This matters from Session 2 on: **records need JDK 16+ and sealed types need 17+.**
  On an old JDK, half of Session 2 will not compile.

- **Maven 3.9 or newer**, from the build-tools session onward. Check with `mvn -v`.
  IntelliJ bundles one, but you want it on your `PATH` too.

- **Internet access on your first Maven build.** Maven downloads what a project depends on,
  once, into `~/.m2/repository` — JUnit for the build-tools session, then AssertJ and Mockito
  for the testing session. The first build of a session can take a few minutes; every
  build after that is offline and instant. If your venue's wifi is unreliable, run the build
  once at home before class — even against the unedited skeleton, which downloads everything
  and then fails, which is fine.

- **IntelliJ IDEA** (Community Edition is fine). This is the editor the course assumes.

- **Comfort with Java classes, interfaces and collections.** If any of that is shaky, work
  through [`sunrise-java-fundermental`](../sunrise-java-fundermental) sessions 1–5 first.

---

## What's in here

```
sessions/session-01-oop-and-enterprise/
  ├── lecture.md      ← start here. Read it top to bottom
  ├── examples/       ← small programs demonstrated in class
  └── src/            ← the exercise, plus the checker you run yourself

sessions/session-02-modern-java-types/
  ├── lecture.md
  ├── examples/       ← four demo programs
  ├── src/            ← the in-class exercise + its checker
  └── homework/       ← a second exercise, with its own checker

sessions/session-03-build-tools-and-functional-java/
  ├── lecture.md
  ├── lambda-drills/  ← a real MAVEN PROJECT. 22 drills, checked by JUnit
  └── homework/       ← a second Maven project, 12 more drills

sessions/session-04-testing-junit-assertj-mockito/
  ├── lecture.md
  ├── order-tests/    ← a Maven project: thirteen tests for you to write
  └── homework/       ← three behaviours nobody tested, and no instructions
```

From Session 3 on, the exercise folder is named after the project inside it — the
`<artifactId>` in its `pom.xml`, the same way any Maven project on disk is named. So it is
`lambda-drills/`, not `src/`; the `src/` inside it is Maven's own source folder.

| Path | What it is |
|---|---|
| `sessions/session-XX-…/lecture.md` | The guided lesson. This is the main text |
| `.../examples/` | Programs demonstrated in class. Yours to replay, never graded |
| `.../src/` | *(Sessions 1–2)* The exercise. Almost everything is written for you |
| `.../src/Verify.java` | *(Sessions 1–2)* The checker. PASS/FAIL per check. **Don't edit it** |
| `.../lambda-drills/`, `.../order-tests/` | *(Session 3 on)* The exercise, as a named Maven project |
| `.../<project>/pom.xml` | *(Session 3 on)* The Maven build. Sometimes it has TODOs for you |
| `.../homework/` | A second exercise, in its own domain, done afterwards |
| `slides/` | The slide decks, if you want to review what was on screen |

---

## The sessions so far

| Session | Topic | You run | Checks |
|---|---|---|---|
| [1](sessions/session-01-oop-and-enterprise/lecture.md) | **Java OOP & the Enterprise Context** — the four pillars honestly, program to an interface, constructor injection, Jakarta EE and where Spring sits | `BlastRadius`, `DispatchDemo` | 8 |
| [2](sessions/session-02-modern-java-types/lecture.md) | **Modern Java Types** — `var`, records, compact constructors, sealed types, text blocks | `VarLab`, `RecordAnatomy`, `SealedProof`, `TextBlockLab` | 24 + **26 homework** |
| [3](sessions/session-03-build-tools-and-functional-java/lecture.md) | **Build Tools and Functional Java** — Maven, Gradle, lambdas, streams and collectors | — *(demos are typed live)* | 22 + **12 homework** |
| [4](sessions/session-04-testing-junit-assertj-mockito/lecture.md) | **Testing in Java** — `Optional` catch-up, then JUnit 5, AssertJ, Mockito, and why a green suite proves nothing | — | 13 tests, then 3 more |

---

## How the sessions were reordered

If you have an older copy of the plan, or you noticed Session 2 promising something that has
not arrived: the order changed in August 2026, deliberately.

**It used to be:** modern Java II (lambdas, streams, pattern matching) → testing → build tools.

**It is now:** build tools + lambdas and streams → testing → the Spring container.

**Why.** Testing was being taught before there was a build tool, which meant running JUnit
through a hand-downloaded jar and a shell script — an arrangement nobody uses at work and one
that teaches a student nothing they can reuse. Maven now arrives *first*, so the testing
session is an ordinary Maven project with three dependencies, exactly like every real one.
Lambdas and streams moved forward with it because they give you something worth testing, and
because they are what the drills in that session practise.

**Then, in September 2026, pattern matching and virtual threads were cut** to give Spring the
slot. The course is 13 sessions now, and Module 1 ends after testing.

**One consequence, stated honestly.** Session 2 ends by promising that the `instanceof` chain
and its unreachable `throw` get deleted "next session". They do not, in this course. Look up
`switch` pattern matching over a sealed interface on your own — the annoyance you felt writing
that chain is exactly the argument for it, and it is an evening's reading.

---

## How you run things

**Sessions 1 and 2 — you edit in IntelliJ, you run from a terminal.**

Those sessions are folders of plain `.java` files with no build tool. In that arrangement
IntelliJ cannot reliably work out which class to launch, so its green run arrows either aren't
there or start the wrong thing.

You don't have to leave IntelliJ — it has a terminal built in:
**View → Tool Windows → Terminal**, or **Alt+F12**.

| What you want to run | Command |
|---|---|
| Any demo program | `java BlastRadius.java`, `java RecordAnatomy.java`, … — from `examples/` |
| The exercise checker | `javac *.java` then `java Verify` — from `src/` *(Sessions 1–2)* |
| The exercise application | `java Main` — from `src/`, after compiling *(Sessions 1–2)* |
| The exercise tests | `mvn test` — from the project folder, e.g. `lambda-drills/` or `order-tests/` *(Session 3 on)* |
| The homework checker | `javac *.java` then `java Verify` — from `homework/` |

A single `.java` file runs directly, with no compile step. A *folder* of them needs `javac`
first.

**Session 3 onward — the run buttons work.**

The exercise is a Maven project, so there is a project model, so IntelliJ knows where your
code is and what it depends on.

> **Open the `pom.xml`, not the folder.** **File → Open**, select the `pom.xml` file itself,
> and choose *Open as Project*. Opening the containing folder gives you an IDE that knows
> nothing, and it is the single most common way to lose ten minutes in these sessions.

| What you want to run | How |
|---|---|
| All the tests | Right-click `src/test/java` → **Run 'All Tests'**, or `mvn test` |
| One test class | The green arrow beside its name, or `mvn test -Dtest=OrderServiceTest` |
| One test method | The green arrow beside it, or `mvn test -Dtest=OrderTest#rejectsNegativeTotal` |
| A Maven phase | The **Maven** tool window (right-hand edge) → **Lifecycle** |

The demo programs in `examples/` still run as single files — `java RecordAnatomy.java` —
because they have no dependencies. **Sessions 3 and 4 have no `examples/` folder**: Session 3's
two demonstrations are typed live in the session, and Session 4 has no demo at all. The lecture
carries those points in full.

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

Sessions 3 and 4 have no `examples/` folder: Session 3's two demonstrations are typed live so
you watch the compiler react, and Session 4 has no demo at all — its arguments are made in
prose and on the slides.

**3. Do the exercise.** It is in `src/` for Sessions 1–2, and in the session's named Maven
project folder from Session 3 on.

These are fill-ins, not build-from-scratch. Given files are marked, and every file you *do*
touch tells you exactly what to write in a comment at the top.

**4. Check your own work.**

Sessions 1 and 2: `javac *.java` then `java Verify`, after every edit. Session 3 onward:
`mvn test`, or the run button.

Each check is PASS or FAIL. When all of them pass, you're done. There is no "ask the instructor
whether it's right" step — the checker tells you.

> **Session 4 is different, and it is the point of that session.** Its thirteen unwritten
> tests fail on purpose; delete the `fail(...)` line without writing the test and it passes
> empty, which is what a green suite is worth. You are finished when all thirteen are green
> **and** you have broken the production code three times and watched a test go red each time.

**5. If you finish early**, each lecture ends with optional extensions. They aren't checked and
they're worth doing.

**6. Do the homework.** It lives in `homework/`, has its own domain, and is not a continuation —
if the in-class exercise went badly you can still do it cleanly. Read `homework/README.md`
first.

---

## If something goes wrong

**Nothing compiles when you first open the exercise.** *(Sessions 1–2)*
That is expected, and the lecture says exactly how many errors to expect and why. Session 1:
one error, about a missing constructor. Session 2: nine errors, from two causes.

**Your shell doesn't understand `javac *.java`.** *(Sessions 1–2)*
Some Windows shells don't expand the `*`. List the files instead — the lecture spells out the
full command.

**A text-block check fails and the two strings look identical.** *(Session 2)*
It's whitespace. `Verify` says so explicitly when that's the only difference. Count the leading
spaces — your closing `"""` is in the wrong column — then count the `\n` at the end.

**`package org.junit.jupiter.api does not exist`.** *(Session 3 on)*
That is a **build** error, not a code error. A dependency is missing from the `pom.xml` — check
the TODO blocks in it.

**`records are not supported in -source 8`.** *(Session 3)*
Also the build: the `maven.compiler.release` property is missing from the POM.

**`package org.assertj.core.api does not exist`, or `package org.mockito does not exist`.** *(Session 4)*
Build errors again, and expected: the `pom.xml` ships with two `TODO` blocks for you to fill in.
AssertJ is the first, Mockito the second.

**A test fails with `TODO: write this test`.** *(Session 4)*
That is the skeleton, not your code. Thirteen tests fail on purpose until you write them —
delete the `fail(...)` line as you write each one.

**`expected <180.0> but was <180.00000000000003>`.** *(Session 4)*
Floating-point arithmetic, not a bug in your test. Compare doubles with a tolerance:
`isEqualTo(180.00, within(0.001))`.

**`Wanted but not invoked`, or `UnnecessaryStubbingException`.** *(Session 4)*
Mockito. The first means a `verify` did not happen — usually you stubbed a different outcome
than the test needs. The second means you stubbed a call the code never makes, usually a
`when(...)` copy-pasted from the test above.

**IntelliJ shows no green arrows and doesn't recognise your own classes.** *(Session 3 on)*
You opened the folder instead of the `pom.xml`. Close the project and re-open the POM itself.

**The first `mvn test` sits there for minutes.**
It is downloading. That happens once. If it fails outright, you have no route to Maven Central —
check your network, or a corporate proxy.

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

**Java 25 · Maven.** Later sessions add **Spring Boot 4.0.3**. Gradle is shown once, for
comparison; everything built here uses Maven. Don't mix versions.
