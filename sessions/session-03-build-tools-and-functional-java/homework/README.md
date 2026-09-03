# Homework — Bookshop Drills

**12 checks, in two sets. Budget 45–60 minutes.**

A separate Maven project, in a different domain from the in-class exercise. It
does **not** continue from it — if class went badly, start here clean.

## What you are doing

The same deal as in class: each drill shows a loop that already works, and you
replace it with a collector or a pipeline. What is new is where the drills end
up. In class, streams produced a list or a number. Here they produce a **`Map`**,
which is what a reporting method in a real service actually returns.

| File | Set | Drills | Focus |
|---|---|---|---|
| `GroupingDrills.java` | D | 6 | `groupingBy`, downstream collectors (`counting`, `averagingDouble`, `mapping`, `maxBy`), `partitioningBy` |
| `AdvancedDrills.java` | E | 6 | `flatMap`, `joining`, `summarizingDouble`, `toMap` with a merge function, a computed group key, streaming a `Map` |

**Do not modify** `model/Book.java`, `data/Catalogue.java`, or anything under
`src/test/`.

## The two that will take the longest

- **D6 — `bestSellerByGenre`.** You need `Collectors.maxBy(...)` as the
  downstream, which hands you an `Optional` per group, and then
  `Collectors.collectingAndThen(...)` to unwrap it. Expect to look it up. That
  is a normal day with collectors, not a failure.
- **E4 — `copiesSoldByAuthor`.** `Collectors.toMap` with only two arguments
  throws `IllegalStateException: Duplicate key` the moment two books share an
  author. **Try it that way once on purpose**, read the exception, then add the
  third argument. You will meet this bug in production code one day and you
  will recognise it in five seconds instead of forty minutes.

## Running the checks

Open this folder's `pom.xml` as a project in IntelliJ (**File → Open**, select
the `pom.xml`, *Open as Project*), then right-click `src/test/java` → **Run 'All
Tests'**. Or, in a terminal beside the POM:

```
mvn test
```

This POM is **complete** — no TODOs. You did that part in class.

## What "done" looks like

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 12 fail before you start.

## Two things to try afterwards, unchecked

**A.** `titlesByDecade()` returns the decades in order because you chose the
   `Map` implementation. Take that choice out — go back to the two-argument
   `groupingBy` — and run it a few times. Does the order change? Does it change
   *between runs*? Write down what you would have assumed if the test had not
   been there.

**B.** `averagePriceByGenre()` gives `Non-fiction = 18.413333333333334`. Money is
   not a `double`, and this is the reason. Look up `BigDecimal`, and write one
   sentence on what you would change if these were real prices in a real shop.
