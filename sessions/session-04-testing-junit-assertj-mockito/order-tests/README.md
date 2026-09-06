# Exercise — Testing the Order Service

**13 tests to write.**

Everything under `src/main/java` is **given, complete and working**. You do not change it.
Your job is the folder next to it.

## First, run the suite

Before you write anything:

```
mvn test
```

You will see:

```
Tests run: 17, Failures: 13, Errors: 0
BUILD FAILURE
```

**Thirteen red, four green.** The thirteen are the tests you are about to write: each one
calls `fail("TODO: write this test")` so it cannot pass by accident. Delete that line as you
write each test.

Then do this once, because it is the single most useful thing in this session. Delete the
`fail(...)` from one test, leave the body empty, and run again — **17 green, BUILD SUCCESS**.
A test method with no body passes. Put the line back. Green means "nothing threw", not "the
code is right", and outside this exercise nothing puts a `fail(...)` there for you.

## Then finish the POM

Two `TODO` blocks: **AssertJ** and **Mockito**. Same four lines each, the same shape you
wrote last session. A testing library is not special; it is a dependency with `test` scope.

Skip them and you get `package org.assertj.core.api does not exist`, which is a build
error, not a code error.

## What you are writing

| File | Tests to write | What it is really about |
|---|---|---|
| `OrderTest.java` | 5 | asserting on a **value** — and on a **refusal**, with `assertThatThrownBy` |
| `OrderReporterTest.java` | 4 | asserting on a **calculation**, including the empty case everyone skips |
| `OrderServiceTest.java` | 4 | asserting on a **call that happened**, with Mockito's `verify` |

Two tests in `OrderTest` and one in each of the others are written for you as models. Keep
their shape:

```java
Order order = new Order("chan@example.com", 200.00, 1);   // Arrange

Order discounted = order.withDiscount(10);                 // Act

assertThat(discounted.total()).isEqualTo(180.00, within(0.001));   // Assert
```

Three blocks, blank lines between them, and one behaviour per test. A test you can read in
three seconds is worth more than a clever one.

## Then watch them fail

A green suite you have never seen red proves nothing. Once the thirteen tests are written,
break the production code on purpose — three times, one at a time, undoing each before the
next:

| Break this | In | Expect red |
|---|---|---|
| delete the `total < 0` check | `Order` | "rejects a negative total" |
| swap `revenue` and the average in the `new SalesReport(...)` call | `OrderReporter` | a revenue or average test |
| delete the `sendConfirmation(...)` line | `OrderService` | the email test — and **only** a `verify` can see this one |

Each break should turn exactly one test red, and its name should tell you what you broke. If
the suite stays green, that behaviour has no test yet, whatever the file looks like.

**Put every change back when you are done.**

## Running things

**In IntelliJ.** Open the `pom.xml` as a project. Right-click `src/test/java` → **Run 'All
Tests'**, or click the green arrow beside any single test.

**In a terminal**, beside the `pom.xml`:

```
mvn test                              # everything
mvn test -Dtest=OrderServiceTest      # one class
```

## If you get stuck

| What you see | What it means |
|---|---|
| `package org.assertj.core.api does not exist` | POM TODO 1 |
| `package org.mockito does not exist` | POM TODO 2 |
| A test passes but you have not written it | You deleted its `fail(...)` and left the body empty. An empty test passes — that is the point |
| `expected <180.0> but was <180.00000000000003>` | Never compare doubles with `isEqualTo(x)` alone. Use `within(0.001)` |
| `Expecting code to raise a throwable, but nothing was thrown` | Your `assertThatThrownBy` lambda did not actually call the thing, or the code no longer refuses |
| `Wanted but not invoked` | `verify` says the call never happened. Either the code does not do it, or you stubbed the wrong result |
| `UnnecessaryStubbingException` | You stubbed something the code never called. Usually a copy-pasted `when(...)` from the previous test |
| You broke the code and nothing went red | That behaviour has no test yet. Work out which one it should have been |

## Finished early?

**A.** Write one more test in `OrderTest` that fails, on purpose, and read the AssertJ failure
   message carefully. Then break it a different way. Failure messages are a library's real
   user interface, and AssertJ's are the reason people move to it.

**B.** In `OrderServiceTest`, add `verify(gateway).charge(order)` to the declined test. It
   passes. Now ask whether it was worth writing — and what a test that asserts every single
   interaction costs you the next time somebody refactors the class.

**C.** `doesNothingWhenOutOfStock` and `doesNothingOnDecline` are nearly the same test. Look
   up `@ParameterizedTest` with `@MethodSource` and write them as one. Then decide whether
   the result is clearer. It is not always.
