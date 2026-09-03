# Exercise — Lambda and Stream Drills

**22 checks, in three sets.** This is the first exercise in the course that is a
real Maven project, and the first whose checks are real JUnit tests.

You do not have to understand the test files yet — you only run them. Writing
them is the next session.

## What you are doing

Every drill shows you code that **already works**, written the way Java forced
you to write it before Java 8: an anonymous inner class, or a loop. Your job is
to produce the same result with a lambda or a stream.

Each one fits on one line. If yours is running to six, stop and look at the
imperative version again — it tells you exactly which operations you need.

| File | Set | Drills | Focus |
|---|---|---|---|
| `LambdaDrills.java` | A | 8 | `Predicate`, `Function`, `Consumer`, `Supplier`, `Comparator`, your own functional interface, capture, `.and(...)` |
| `StreamDrills.java` | B | 7 | `filter`, `map`, `sorted`, `distinct`, `limit`, and chaining them |
| `ReportDrills.java` | C | 7 | `count`, `sum`, `average`, `max`, `allMatch`, `joining`, `groupingBy` |

**Do not modify** `model/Employee.java`, `data/SampleData.java`, or anything
under `src/test/`. Everything you write goes in the three `*Drills.java` files.

## Before the drills — finish the POM

Open `pom.xml`. It has two `TODO` blocks, and the project does not build until
both are filled in. This is the build-tools half of the session, done by hand
once:

| TODO | What it is | What you see if you skip it |
|---|---|---|
| 1 | the `maven.compiler.release` property | `records are not supported in -source 8` |
| 2 | the JUnit 5 test dependency, with `<scope>test</scope>` | `package org.junit.jupiter.api does not exist` |

Both errors are in the *build*, not in your code. Read them once on purpose —
recognising a build error for what it is saves you an hour the first time it
happens for real.

## Running the checks

**In IntelliJ.** Open the `pom.xml` as a project (**File → Open**, select the
`pom.xml`, choose *Open as Project*). IntelliJ reads the POM, works out the
source roots, and from that moment the green arrows in the gutter work — the
thing that was missing for the last two sessions.

- Right-click `src/test/java` → **Run 'All Tests'**.
- Or open one test class and click the green arrow beside its name to run just
  that set.
- Or open the **Maven** tool window (right-hand edge) and double-click
  **Lifecycle → test**.

**In a terminal**, if you prefer, from the folder containing `pom.xml`:

```
mvn test                    # everything
mvn test -Dtest=LambdaDrillsTest    # one set
```

## What "done" looks like

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Before you start, all 22 fail. That is expected and it is the point — every
drill returns `null` or `0` until you write it. Work down the file and watch
the red turn green.

Suggested pacing in class: **Set A, then Set B.** Set C if you get there. Any
drill you do not finish is yours to finish at home; the homework is separate
and does not depend on it.

## If you get stuck

| What you see | What it means |
|---|---|
| `package org.junit.jupiter.api does not exist` | TODO 2 in the POM — the JUnit dependency is missing |
| `records are not supported in -source 8` | TODO 1 in the POM — the compiler release property is missing |
| `Result should not be null` in a test | that drill still returns `null` — you have not written it yet |
| `local variables referenced from a lambda expression must be final or effectively final` | you assigned to a variable the lambda reads. Stop reassigning it |
| `incompatible types: Stream<String> cannot be converted to List<String>` | your pipeline has no terminal operation. Add `.toList()` |
| `Discount is not a functional interface` | you added a second abstract method to it. A lambda needs exactly one |
| `cannot find symbol: method getName()` | `Employee` is a **record**. The accessor is `name()`, not `getName()` |
| IntelliJ shows no green arrows and does not know what `Employee` is | you opened the folder instead of the `pom.xml`. Close the project and re-open the POM itself |

## Finished early?

Not checked by the tests, so no safety net — which is the point.

**A.** Rewrite `topThreeEarnerNames()` so it does **not** sort the whole list.
   Nine names sorted to keep three is wasted work. What does the pipeline look
   like if you never sort at all? (Hint: there is no clean answer with the
   operations you know today. Say what you would need.)

**B.** `distinctCitiesSorted()` — swap `.distinct()` and `.sorted()` around. Same
   answer? Same amount of work? Which order would you want on a million rows?

**C.** Write `namesByDepartment()` a second time as a plain loop, then put the two
   side by side and count: lines, variables you had to name, and places a typo
   could hide. Both are valid Java. Decide for yourself which you would rather
   maintain — and notice that this is a judgement, not a rule.
