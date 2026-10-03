# Java Enterprise Spring Framework — Core

Modern Java → the Spring container → REST APIs → a real database → shipped in a container.

> **Sessions 1 to 9 are available now.** The rest of the course is being written and will land
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

- **PostgreSQL, installed and running — but not until Session 10.** Session 9 uses H2, a database
  that runs inside your application, so there is nothing to install for it. Install PostgreSQL
  before the session after.

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

sessions/session-05-spring-core-di/
  ├── lecture.md
  ├── lending-desk/   ← a README and NOTHING ELSE. You generate the project and write every class
  └── homework/       ← a second project, generated from scratch, six more checkpoints

sessions/session-06-spring-mvc/
  ├── lecture.md
  └── lending-desk/   ← a README and a src/test tree to copy into your Session 5 project

sessions/session-07-rest-apis/
  ├── lecture.md
  └── recipe-api/     ← a README with every class and test in it. You generate the project in class —
                        and KEEP it: the Recipe API is the project for the rest of the course

sessions/session-08-boot-layers-and-aspects/
  ├── lecture.md
  ├── starting-point/ ← last session's finished Recipe API. Copy it if yours is not finished
  └── recipe-api/     ← a README: Lombok, MapStruct and one aspect, three checkpoints

sessions/session-09-hibernate-and-jpa/
  ├── lecture.md
  └── recipe-api/     ← last session's finished Recipe API, ready to work in — plus a README with
                        four checkpoints, and EXTRA-tests.md for after class
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
| `.../lambda-drills/`, `.../order-tests/` | *(Sessions 3–4)* The exercise, as a named Maven project |
| `.../lending-desk/README.md` | *(Session 5)* A **guide, not a project.** You generate the project from `start.spring.io` and write every class yourself |
| `.../recipe-api/README.md` | *(Session 7)* A complete walkthrough, from generating the project to the last green test. Every class and every test is in it, to copy and run |
| `.../starting-point/` | *(Session 8)* The previous session's finished project. Copy it if yours is not finished |
| `.../recipe-api/` | *(Session 9)* The previous session's finished project **and** this session's README, in one folder. Work in it directly |
| `.../recipe-api/EXTRA-tests.md` | *(Session 9)* Optional tests against a real database, for after class |
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
| [5](sessions/session-05-spring-core-di/lecture.md) | **Spring Framework & Dependency Injection** — the container, what Spring Boot adds, generating your own project, stereotypes and the root package rule, constructor injection, scopes and lifecycle, two beans of one interface | — *(demos are typed live)* | 7 checkpoints + **6 homework** |
| [6](sessions/session-06-spring-mvc/lecture.md) | **Spring MVC** — one starter and a server nobody configured, the `DispatcherServlet`, how a URL finds your method, resolvers and message converters, the request lifecycle, thread-per-request, `MockMvc` and the `@WebMvcTest` slice | `mvn test` | 9 tests · homework: draft your personal project |
| [7](sessions/session-07-rest-apis/lecture.md) | **REST APIs — Design, Validation & Errors** — REST as constraints, resource URLs, safe and idempotent methods, status codes on purpose with `ResponseEntity`, controller → service → repository layers (service as interface + `Impl`), request and response DTOs, Bean Validation and `@Valid`, one error shape with `ProblemDetail` and `@RestControllerAdvice`. **Starts the Recipe API, which you keep until Session 13** | `mvn test`, `curl` | 14 tests, all given — copy, run, go green · no homework |
| [8](sessions/session-08-boot-layers-and-aspects/lecture.md) | **Lombok, MapStruct & Aspects** — the constructor and the getters you stop writing, a mapper that is an interface and why a forgotten field becomes a compile error, then aspect-oriented programming: advice, pointcuts, the proxy Spring puts around your service, and where filters and interceptors stand by comparison | `mvn test`, `curl` | 16 tests · 3 checkpoints · no homework |
| [9](sessions/session-09-hibernate-and-jpa/lecture.md) | **Hibernate & JPA Mapping** — JPA as the specification and Hibernate as the implementation, the datasource and a connection pool, entities and why they cannot be records, a one-to-many and its owning side, a Spring Data repository that nobody writes, **derived queries** from a method name, **pagination and sorting**, and the N+1 problem read in the SQL log. **The Recipe API moves from a map to a database (H2)** | `mvn test`, `curl`, the H2 console | 12 tests, green at all 4 checkpoints · no homework · extra tests for after class |

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
because they have no dependencies. **Sessions 3, 4 and 5 have no `examples/` folder**: the
demonstrations in Sessions 3 and 5 are typed live in the session, and Session 4 has no demo at
all. The lecture carries those points in full.

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

Sessions 3, 4 and 5 have no `examples/` folder: the demonstrations in Sessions 3 and 5 are
typed live so you watch the compiler — and the container — react, and Session 4 has no demo at
all; its arguments are made in prose and on the slides.

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

**From Session 7 there is no homework.** The in-class exercise is the whole deliverable; if it
does not finish in the room, finish it in your own time. From Session 7 the exercise also
**carries on** — Session 7 starts the Recipe API and every session up to 13 builds on it. If you
fall behind, each session from 8 on ships where the previous session ended: Session 8 as a
`starting-point/` folder to copy, Session 9 as the `recipe-api/` folder itself, which you work in
directly.

**From Session 9 no test is written in class.** The tests you already have stay green while
everything underneath them changes — that is what they are for. What you add, you check with
`curl`, the SQL log and the database itself.

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

**`NoSuchBeanDefinitionException` for a class you definitely annotated.** *(Session 5)*
The container never scanned it. `@SpringBootApplication` scans **its own package and everything
underneath**, so a class outside that tree is invisible — and the message says nothing at all
about packages. Check where the file lives before you check the annotation.

**A wall of red before any of your own code runs.** *(Session 5)*
Normal. A container that will not start takes the whole application down, so one missing
annotation looks like everything being broken. It is **one** problem. Read one message, scroll to
`Caused by:` and read the innermost line, fix the cause, run again.

**The app starts, prints the banner, and exits.** *(Session 5)*
Not a failure. With no web starter there is nothing to keep it alive. If it printed nothing of
yours, your `CommandLineRunner` is not a bean — or it is outside the root package.

**`NoUniqueBeanDefinitionException: expected single matching bean but found 2`.** *(Session 5)*
Two beans implement one interface and nothing broke the tie. **Read the message — it names both
candidates.** You want `@Primary` on the default one, or `@Qualifier` at the injection point.

**Every test fails to compile with `package com.sunrise.recipeapi does not exist`.** *(Session 7)*
Initializr filled the package name in from the artifact and gave you `com.sunrise.recipe_api`,
with an underscore. Type over it when generating, or rename the package. One underscore breaks
all fourteen tests.

**Validation "doesn't work" — a blank field still gets `200` or `201`.** *(Session 7)*
`@Valid` is missing from the controller parameter. The constraints on the record do nothing on
their own. Check this first, always.

**`internalNotes` shows up in a response.** *(Session 7)*
Something still returns `Recipe` instead of `RecipeResponse`. No controller method should mention
`Recipe` at all.

**`415 Unsupported Media Type` from `curl`.** *(Session 7)*
The request has no `Content-Type: application/json` header. Add `-H 'Content-Type: application/json'`.

**`cannot find symbol: method getTitle()` after adding Lombok.** *(Session 8)*
Lombok is on the classpath but not running. The `annotationProcessorPaths` block is missing from
the `maven-compiler-plugin` in `pom.xml`.

**MapStruct says `Unmapped target property`.** *(Session 8)*
Working as designed: with `ReportingPolicy.ERROR` every target field needs a source or an explicit
`ignore = true`. The error names the field.

**Your aspect never fires.** *(Session 8)*
In this order: is `@Component` on it, is the pointcut's package and class name spelled right, and
is the call coming from another bean rather than from the object itself.

**The whole build goes red the moment `Recipe` becomes an entity.** *(Session 9)*
Expected. `Recipe` stopped being a record, so `recipe.id()` and its constructor are gone and the id
is a `Long`. The README lists every file that has to follow, in order; it is green again at the end
of that list.

**`No default constructor for entity`.** *(Session 9)*
`@NoArgsConstructor` is missing from an entity. Hibernate creates the object empty, then fills it.

**`GET /recipes` returns `[]` after you switch to the database.** *(Session 9)*
Correct. The seeded recipes lived in the class you deleted, and H2 starts empty. `POST` some.

**The H2 console says `Database "mem:recipes" not found`, or the page is a 404.** *(Session 9)*
The first: type `jdbc:h2:mem:recipes` into the JDBC URL box, with the application running. The
second: `spring-boot-h2console` is missing from `pom.xml`.

**`No property '…' found for type 'Recipe'` and the application will not start.** *(Session 9)*
A derived query's method name uses a field the entity does not have. Check the spelling in
`RecipeRepository`.

**`curl` prints half an answer, or something like `[1] 12345`.** *(Session 9)*
The URL has an `&` in it and no quotes around it. Put the whole URL in double quotes.

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

**Java 25 · Maven.** Later sessions add **Spring Boot 4.0.8**. Gradle is shown once, for
comparison; everything built here uses Maven. Don't mix versions.
