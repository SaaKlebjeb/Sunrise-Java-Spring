---
layout: section
---

# Testing in Java

Optional · JUnit 5 · AssertJ · Mockito

---
layout: default
---

# Learning Objectives

<v-clicks>

- Use **`Optional`** — `orElseGet`, `map`, `filter` — and say why `null` was worse
- Say what a **test** is, name the **three kinds**, and say why today is entirely units
- Say what a unit test is **for**, in terms of the harness you have been using
- Write a JUnit 5 test — `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested`
- Structure a test as **Arrange, Act, Assert** — and spot the shape breaking
- Assert with **AssertJ**: values, collections, exceptions
- Explain **stub** against **mock**, and why "test double" is the honest word
- Use **Mockito** — `mock`, `when(...).thenReturn(...)`, `verify`, `never()`
- Say what you should **not** mock
- Judge a suite by whether it **catches a broken change** — not by whether it is green

</v-clicks>

---
layout: section
---

# `Optional`

Picking up where last session stopped

---
layout: default
---

# `findFirst` did not hand you an `Employee`

```java
Optional<Employee> first = staff.stream()
        .filter(e -> e.salary() > 100_000)
        .findFirst();
```

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

There may be nobody earning that much.

`findFirst` has to answer honestly, so it returns a **box** that may be empty.

</div>

</v-click>

---
layout: default
---

# What `null` costs you

```java
Employee findByName(String name)
```

<v-clicks>

- Can this return `null`? **The signature does not say**
- You find out from the javadoc, the body, or production
- Forget to check, and the `NullPointerException` fires
  somewhere else entirely, much later

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

`null` is a value that means "no value", and it fits in every type.

That is exactly why the compiler cannot help you.

</div>

</v-click>

---
layout: default
---

# The type stops lying

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## Before

```java
Employee findByName(String name)
```

<div class="mt-3 text-sm opacity-80">
"Returns an employee." Sometimes.
</div>

</div>

<div>

<v-click>

## After

```java
Optional<Employee> findByName(String name)
```

<div class="mt-3 text-sm opacity-80">
"Returns an employee <b>or nothing</b>, and you
must decide which before you can use it."
</div>

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Same code. The difference is that now the **caller cannot forget**.

</div>

</v-click>

---
layout: default
---

# Making one

```java {all|1|2|3}
Optional.of(employee);           // NPE right here if employee is null
Optional.ofNullable(maybeNull);  // empty if it happens to be null
Optional.empty();                // deliberately nothing
```

<v-click>

<div class="mt-8 text-center opacity-80">

Most of the time you do not make them.

You **receive** them — from `findFirst`, `max`, `average`, and from other people's APIs.

</div>

</v-click>

---
layout: default
---

# Getting the value out

<div class="mt-4">

| Call | What you get |
|---|---|
| `orElse(other)` | the value, or `other` |
| `orElseGet(supplier)` | the value, or a **computed** fallback |
| `orElseThrow()` | the value, or `NoSuchElementException` |
| `orElseThrow(supplier)` | the value, or **your** exception |
| `ifPresent(action)` | runs the action only if there is something |
| `ifPresentOrElse(a, b)` | one branch each way |
| `isPresent()` / `isEmpty()` | a plain `boolean` |

</div>

---
layout: default
---

# `orElse` runs either way

```java {all|1|4|5}
Optional<Employee> found = repo.findByName(name);

// expensiveDefault() is CALLED even when found is present
Employee a = found.orElse(expensiveDefault());
Employee b = found.orElseGet(() -> expensiveDefault());
```

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

`orElse` takes a **value**, so the argument is evaluated first — always.

`orElseGet` takes a **lambda**, so it runs only when the box is empty.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center opacity-80">

Cheap constant? `orElse`. A database call, a `new`, anything with a cost? `orElseGet`.

</div>

</v-click>

---
layout: default
---

# Never call `get()` unguarded

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## This

```java
if (found.isPresent()) {
    use(found.get());
}
```

</div>

<div>

<v-click>

## is this

```java
if (e != null) {
    use(e);
}
```

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

You wrapped the value, then unwrapped it to do the null check by hand.

All of the ceremony, none of the benefit.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

`found.ifPresent(this::use);`

</div>

</v-click>

---
layout: default
---

# It behaves like a stream of nought or one

```java {all|1|2|3}
found.map(Employee::department)          // Optional<String>
     .filter(d -> d.equals("Engineering"))
     .ifPresent(System.out::println);
```

<v-clicks>

- `map` — transform the value **if there is one**
- `filter` — keep it only if it also matches, otherwise empty
- Nothing runs at all when the box starts empty. No `if`, no crash

</v-clicks>

---
layout: default
---

# Where `Optional` belongs

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

<div class="border-2 border-green-500/50 rounded-lg px-4 py-3 bg-green-500/10">

## Yes

**Return types** of methods that may genuinely find nothing.

</div>

</div>

<div>

<v-click>

<div class="border-2 border-red-500/50 rounded-lg px-4 py-3 bg-red-500/10">

## No

**Fields** — it is a wrapper on every object, and not serialisable

**Parameters** — write two overloads instead

**Collections** — an empty list already means "nothing"

</div>

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 text-center opacity-80">

Never return an <code>Optional&lt;List&lt;T&gt;&gt;</code>. Return an empty list.

</div>

</v-click>

---
layout: statement
---

# A return type that cannot lie

`null` is a comment the compiler ignores.

`Optional` is the same comment, enforced.

---
layout: default
---

# Pause

<div class="mt-6">

A method looks up an employee by id and the id always comes from a dropdown
the user picked from — so it is **always** found.

</div>

<v-clicks>

- Should it return `Optional<Employee>` or `Employee`?
- What happens the day somebody calls it from a script?

</v-clicks>

---
layout: section
---

# You have already written
# a test framework

---
layout: default
---

# Two sessions of this

```java
public class Verify {
    public static void main(String[] args) {
        check("order keeps its email", order.customerEmail(), "chan@example.com");
        check("total is normalised",   order.total(),          250.0);
        ...
        System.out.println(passed + "/" + checks + " checks passed");
    }
}
```

<v-click>

<div class="mt-6 text-center text-xl text-green-300">

`ALL 24 CHECKS PASSED` — and you believed it. Correctly.

</div>

</v-click>

---
layout: statement
---

# That was a test framework

Small, hand-rolled, good enough to learn with.

It has **four** problems.

---
layout: default
---

# One — no isolation

```java
check("a", ...);
check("b", ...);   // ← this one THROWS
check("c", ...);   // never runs
check("d", ...);   // never runs
```

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

You cannot tell **"one thing is broken"**

from **"everything after line 40 is unknown"**.

</div>

</v-click>

---
layout: default
---

# Two — no names

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## `check 7 FAILED`

Six months from now, you read the code of check 7 to find out what the program was
supposed to do.

</div>

<div>

<v-click>

## `rejects an order with a negative total`

Documentation that **cannot go out of date** —

because it fails when it stops being true.

</v-click>

</div>

</div>

---
layout: default
---

# Three — no setup

<v-clicks>

- Every check builds its own fixtures by hand
- The same four lines appear eleven times
- The constructor changes, and you edit eleven places

</v-clicks>

---
layout: default
---

# Four — no report

<v-clicks>

- It prints to a console
- A build server needs a **file**
- Which tests ran, which failed, how long they took

</v-clicks>

<v-click>

<div class="mt-8 text-center text-xl">

So it can tell you this test started failing on **Tuesday's commit**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

Console output is not a record.

</div>

</v-click>

---
layout: section
---

# What a test is

---
layout: default
---

# Code that runs your code and checks the answer

<v-clicks>

- You **call** the thing
- You **state** what you expected
- If the two do not match, it **fails loudly**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

A program that prints numbers for you to squint at is a **report**.

A test reaches a **verdict**, and nobody has to interpret it.

</div>

</v-click>

---
layout: default
---

# Why bother

<v-clicks>

- It tells you the code works **now**
- It tells you the code still works **after you change it**

</v-clicks>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

The second one is the whole point.

Without tests, *"I refactored it"* and *"I broke it"* look identical
until a customer finds out.

</div>

</v-click>

---
layout: default
---

# Three kinds

<div class="mt-4">

| | What it covers | How fast | How many |
|---|---|---|---|
| **Unit** | one class, stand-ins for everything it talks to | milliseconds | thousands |
| **Integration** | your code plus one real thing — a database, a broker | seconds | hundreds |
| **End-to-end** | the whole system, through its front door | minutes | a handful |

</div>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Each row proves more than the one above it — and runs slower, and tells you
**less about what broke** when it goes red.

</div>

</v-click>

---
layout: default
---

# The shape of a healthy suite

<div class="mt-8 flex flex-col items-center gap-3">

<div class="w-1/3 border-2 border-red-500/50 rounded-lg bg-red-500/10 text-center py-2 text-sm">
End-to-end · a handful · minutes
</div>

<div class="w-2/3 border-2 border-yellow-500/50 rounded-lg bg-yellow-500/10 text-center py-3">
Integration · hundreds · seconds
</div>

<div class="w-full border-2 border-green-500/50 rounded-lg bg-green-500/10 text-center py-4 text-lg">
Unit · thousands · milliseconds
</div>

</div>

<v-click>

<div class="mt-8 text-center opacity-80">

An end-to-end failure says *something, somewhere, is wrong*.

A unit failure names the class and the behaviour.

</div>

</v-click>

---
layout: section
---

# What a unit test is

---
layout: default
---

# One behaviour, in isolation, in milliseconds

<v-clicks>

- No network
- No database
- No filesystem
- No clock

</v-clicks>

---
layout: default
---

# Three properties that matter

<v-clicks>

- **Deterministic** — same result every time, on every machine
- **Independent** — order does not matter
- **A sentence** — the name says what the code should do

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

A test that fails one run in twenty is **worse than no test**.

The team learns to re-run it.

</div>

</v-click>

---
layout: default
---

# One behaviour per test

Not one **method** — one **behaviour**.

<v-click>

<div class="mt-6">

`place()` deserves four tests, because it does four distinguishable things.

</div>

</v-click>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

Not pedantry: when a test fails, its **name** should tell you what broke
without opening it.

</div>

</v-click>

---
layout: default
---

# Which means the name is the deliverable

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

## Tells you nothing

```
testOrder()
testPlace1()
shouldWork()
test_order_service()
```

</div>

<div>

<v-click>

## Tells you what broke

```
rejects a negative total
trims and lower-cases the email
emails the customer on approval
saves nothing when declined
```

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 text-center text-green-300">

Write the name as the sentence you would say to a colleague.

Then make it true.

</div>

</v-click>

---
layout: default
---

# What makes a test flaky

<v-clicks>

- It reads the **clock**, and one day it runs at midnight
- It depends on **another test** having run first
- It hits the **network**
- It relies on the **iteration order** of a `HashMap`
- It uses a **random** value without a fixed seed

</v-clicks>

<v-click>

<div class="mt-6 border-l-4 border-red-500 pl-4">

Every one of them makes a suite people stop believing.

</div>

</v-click>

---
layout: default
---

# Pause — which of these is a unit test?

<v-clicks>

1. Builds an `Order` and checks the total
2. Starts an in-memory database and saves a row
3. Calls the real payment provider's sandbox
4. Runs `OrderReporter` over a list of five orders

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

Two are clear. One is arguable — and it is arguable for a reason worth naming.

</div>

</v-click>

---
layout: section
---

# JUnit 5

---
layout: default
---

# Three lines, and you have all of it

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.1</version>
    <scope>test</scope>
</dependency>
```

<v-click>

<div class="mt-6 text-center">

You wrote exactly this last session.

</div>

</v-click>

---
layout: statement
---

# A testing library is not special

It is a dependency with `test` scope.

It stays out of the jar you ship.

---
layout: default
---

# The shape

```java {all|3-4|6-8}
class OrderTest {

    @BeforeEach
    void setUp() { ... }

    @Test
    @DisplayName("rejects an order with a negative total")
    void rejectsNegativeTotal() { ... }
}
```

---
layout: default
---

# The annotations you need today

| | |
|---|---|
| `@Test` | this method is a test |
| `@DisplayName("...")` | what shows in the report. A **sentence** |
| `@BeforeEach` / `@AfterEach` | before / after **every** test |
| `@BeforeAll` / `@AfterAll` | once per class. `static`. Use sparingly |
| `@Nested` | an inner class grouping related tests |
| `@Disabled("why")` | skipped. Always give the reason |

---
layout: default
---

# Every test gets a brand-new instance

<v-clicks>

- JUnit constructs the test class **again** for each `@Test`
- A field you set in test 1 does not exist in test 2

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

That is where **independence** comes from.

And it is why `@BeforeEach` is not an optimisation — it is how each test gets its fixtures.

</div>

</v-click>

---
layout: default
---

# The order it all runs in

```
@BeforeAll          once, for the whole class
  @BeforeEach       ┐
  test 1            │  a fresh instance
  @AfterEach        ┘
  @BeforeEach       ┐
  test 2            │  another fresh instance
  @AfterEach        ┘
@AfterAll           once
```

<v-click>

<div class="mt-4 border-l-4 border-yellow-500 pl-4 text-sm">

Anything you put in `@BeforeAll` is **shared** between tests. Modify it in one and you
have just made the others depend on the order they ran in.

</div>

</v-click>

---
layout: default
---

# A detail worth one slide

```java
class OrderTest {          // not public
    @Test
    void rejectsNegativeTotal() { ... }   // not public
}
```

<v-click>

<div class="mt-8 text-center">

Package-private is the convention in JUnit 5.

That is why your test files have no `public` on them.

</div>

</v-click>

---
layout: default
---

# Arrange, Act, Assert

<div class="grid grid-cols-3 gap-4 mt-8">

<div class="border-2 border-blue-500/50 rounded-lg px-4 py-4 bg-blue-500/10">

## Arrange

<div class="mt-2 text-sm opacity-80">

*Given what?*

</div>

<div class="mt-3 text-sm">

Build the situation

</div>

</div>

<div class="border-2 border-amber-500/50 rounded-lg px-4 py-4 bg-amber-500/10">

## Act

<div class="mt-2 text-sm opacity-80">

*When what happens?*

</div>

<div class="mt-3 text-sm">

Call the one thing

</div>

</div>

<div class="border-2 border-green-500/50 rounded-lg px-4 py-4 bg-green-500/10">

## Assert

<div class="mt-2 text-sm opacity-80">

*Then what must be true?*

</div>

<div class="mt-3 text-sm">

State the expectation

</div>

</div>

</div>

<v-click>

<div class="mt-8 text-center opacity-80">

Three blocks. Blank lines between them. Every time.

</div>

</v-click>

---
layout: default
---

# One test, three blocks

```java {all|4|6|8}
@Test
@DisplayName("takes a percentage off the total")
void appliesDiscount() {
    Order order = new Order("chan@example.com", 200.00, 1);

    Order discounted = order.withDiscount(10);

    assertEquals(180.00, discounted.total(), 0.001);
}
```

<v-click>

<div class="mt-6 text-center opacity-80">

No comments needed. The blank lines say which block is which.

</div>

</v-click>

<v-click>

<div class="mt-3 text-center text-sm opacity-70">

Plain JUnit — `assertEquals`. The third argument is a tolerance; that one gets its own slide later.

</div>

</v-click>

---
layout: default
---

# Arrange — build the situation, and nothing more

<v-clicks>

- The object under test, its inputs, its stand-ins
- Every value **fixed and written here** — no clock, no random, no database
- A reader must be able to predict the expected answer from this block alone

</v-clicks>

<v-click>

<div class="mt-8 border-l-4 border-blue-500 pl-4">

Fifteen lines of Arrange is **design feedback**: the class is hard to construct.

The same Arrange in every test belongs in `@BeforeEach`.

</div>

</v-click>

---
layout: default
---

# Act — one call, the one the test is named after

<v-clicks>

- **One line.** Capture the return value in a variable
- That line is what the `@DisplayName` describes
- `void` method? Act is still one line — the evidence moves to Assert

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Need two calls? Either one of them is really **Arrange** —

or you are testing two behaviours and owe yourself two tests.

</div>

</v-click>

---
layout: default
---

# Assert — what must now be true

<v-clicks>

- Assertions talk about what **Act** produced
- **Never call the code under test again here** — that is a second Act
- Every assertion in one test is about **one** behaviour

</v-clicks>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

Assertions about the total, the email *and* the audit log:

three reasons to fail, one name.

</div>

</v-click>

---
layout: default
---

# Given, When, Then

<div class="mt-6 text-center text-xl">

Same three blocks. BDD vocabulary.

</div>

<v-click>

<div class="mt-8 text-center">

You meet it in Spring's docs and in Mockito's `given(...).willReturn(...)`.

</div>

</v-click>

<v-click>

<div class="mt-8 text-center text-green-300">

Different words, one pattern. Nobody has to choose.

</div>

</v-click>

---
layout: default
---

# Exception tests fuse Act and Assert

```java {all|1|3-4}
Order order = new Order("chan@example.com", 200.00, 1);

assertThrows(IllegalArgumentException.class,
        () -> order.withDiscount(150));
```

<v-click>

<div class="mt-6 text-center opacity-80">

The call has to happen **inside the lambda** — it throws instead of returning.

</div>

</v-click>

<v-click>

<div class="mt-3 text-center text-sm text-red-300">

Outside it, the exception escapes and the test **errors** instead of passing.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center">

Arrange still stands on its own.

</div>

</v-click>

---
layout: default
---

# With mocks, the blocks still hold

<div class="text-sm opacity-70 mb-2">A look ahead — Mockito arrives after AssertJ</div>

```java {all|1|3|5}
when(gateway.charge(order)).thenReturn(new Approved("TX-1"));

PaymentResult result = service.place(order);

verify(repository).save(order);
```

<v-clicks>

- Stubbing is **Arrange** — it is part of the situation
- `place(order)` is **Act** — still one call
- `verify` is **Assert** — an expectation about a call, not a value

</v-clicks>

---
layout: default
---

# Five ways it goes wrong

| Smell | Why it hurts |
|---|---|
| An assertion in Arrange | you are testing the fixture |
| Two Act lines | the failure no longer names one behaviour |
| No Act at all | you are testing the constructor, whatever the name says |
| The call repeated in Assert | two executions; red tells you which? |
| Arrange reads a clock or a random | not deterministic — one run in twenty lies |

<v-click>

<div class="mt-6 text-center text-green-300">

If your Act block has four lines, you are probably testing two things.

</div>

</v-click>

---
layout: default
---

# `@Nested`

```java
@DisplayName("Order")
class OrderTest {

    @Nested
    @DisplayName("when it is built")
    class Construction { ... }

    @Nested
    @DisplayName("when the arguments are wrong")
    class Validation { ... }
}
```

---
layout: default
---

# The report becomes an outline

```
Order
  when it is built
    ✔ keeps the values it was given
    ✔ trims and lower-cases the email
  when the arguments are wrong
    ✔ rejects a negative total
    ✔ rejects a quantity below one
```

<v-click>

<div class="mt-6 text-center text-xl text-green-300">

A specification of the class, generated from tests that are currently **true**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center opacity-80">

The closest thing to documentation that cannot rot.

</div>

</v-click>

---
layout: section
---

# AssertJ

---
layout: default
---

# JUnit ships assertions of its own

```java
assertEquals(180.00, discounted.total(), 0.001);
```

<div class="mt-2 text-sm opacity-70">

The line you wrote in the three-block example.

</div>

<v-click>

<div class="mt-6 text-center">

They work. Two things about them wear badly.

</div>

</v-click>

---
layout: default
---

# One — which way round is it?

```java
assertEquals(expected, actual);
assertEquals(actual, expected);
```

<v-click>

<div class="mt-6 text-center text-xl">

It is **expected first**.

</div>

</v-click>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Get it backwards and the test still passes or fails **correctly** —

but the failure message has expected and actual the wrong way round,

and you debug the wrong side for ten minutes.

</div>

</v-click>

---
layout: default
---

# Two — the failure messages

<div class="mt-6 text-center text-xl">

Which is the real reason.

</div>

<v-click>

```java
import static org.assertj.core.api.Assertions.assertThat;

assertThat(discounted.total()).isEqualTo(180.00, within(0.001));
```

</v-click>

<v-click>

<div class="mt-6 text-center text-green-300">

Reads left to right. **Actual first, always.**

</div>

</v-click>

---
layout: default
---

# Values

```java
assertThat(order.customerEmail()).isEqualTo("chan@example.com");
assertThat(order.total()).isEqualTo(180.00, within(0.001));
assertThat(report.orderCount()).isEqualTo(3);
assertThat(result).isInstanceOf(Approved.class);
assertThat(result).isNotNull();
assertThat(message).contains("negative");
```

---
layout: default
---

# Collections

```java {all|1|2-3|4|5}
assertThat(orders).hasSize(3);
assertThat(names).containsExactly("Alice", "Bob");
assertThat(names).containsExactlyInAnyOrder("Bob", "Alice");
assertThat(orders).extracting(Order::customerEmail).contains("chan@example.com");
assertThat(orders).isEmpty();
```

<v-click>

<div class="mt-6">

`extracting(...)` turns a list of objects into a list of one field — so you assert on the
interesting part instead of writing `equals` for a whole object.

</div>

</v-click>

---
layout: default
---

# Exceptions

```java
assertThatThrownBy(() -> new Order("nope", 10.00, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("customerEmail");
```

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

It takes a **lambda**, because the exception has to be thrown somewhere AssertJ can catch it.

Call the method outside the lambda and the exception escapes: the test **errors**, it does
not pass.

</div>

</v-click>

---
layout: default
---

# `within(...)` on every double

<div class="mt-4 text-lg">

The tolerance the JUnit example left unexplained.

</div>

<v-clicks>

- A `double` is stored in binary; most decimal fractions have no exact binary form
- So `0.1 + 0.2` is not `0.3`, and a ten-percent discount off 200 gives `180.00000000000003`
- An exact-equality assertion fails on that — **correctly**. The numbers really do differ
- A tolerance says *equal to the precision I care about*
- Same idea in both libraries: JUnit's third argument, AssertJ's `within(...)`
- Pick one smaller than any difference that would matter — `0.001` on money is a tenth of a cent

</v-clicks>

<v-click>

<div class="mt-6 border-l-4 border-yellow-500 pl-4 text-sm">

Money in a real system belongs in `BigDecimal` — that is the persistence course.
The habit of a tolerance starts today.

</div>

</v-click>

---
layout: default
---

# Where AssertJ actually pays

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

## `assertEquals`

Two lists, printed end to end.

You find the difference.

</div>

<div>

<v-click>

## `assertThat`

Which elements were **missing**.

Which were **unexpected**.

In what **position**.

For a record: the **component** that differs.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 text-center text-green-300">

Not a nicer syntax. Ten minutes you do not spend reading two long lines
that differ in one character.

</div>

</v-click>

---
layout: default
---

# When one assertion is not enough

```java
assertThat(report.orderCount()).isEqualTo(3);
assertThat(report.totalRevenue()).isEqualTo(550.00, within(0.001));   // fails here
assertThat(report.topCustomer()).isEqualTo("chan@example.com");       // never runs
```

<v-click>

<div class="mt-6">

The first failure ends the test. You fix it, re-run, and find the next one.
Three round trips to learn three things.

</div>

</v-click>

<v-click>

```java
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(report.orderCount()).isEqualTo(3);
    softly.assertThat(report.totalRevenue()).isEqualTo(550.00, within(0.001));
    softly.assertThat(report.topCustomer()).isEqualTo("chan@example.com");
});
```

<div class="mt-3 text-center text-green-300">All three checked. All failures reported at once.</div>

</v-click>

---
layout: default
---

# And when the failure still is not obvious

```java
assertThat(report.totalRevenue())
        .as("revenue for %d orders", report.orderCount())
        .isEqualTo(550.00, within(0.001));
```

<v-click>

<div class="mt-8 text-center">

`as(...)` puts your sentence at the front of the failure message.

Worth it on the assertion that will confuse somebody at 2am — which is usually
the one inside a loop or a parameterized test.

</div>

</v-click>

---
layout: default
---

# Pause

Your test says:

```java
assertThat(names).containsExactly("Alice", "Bob");
```

It fails. The actual list was `["Bob", "Alice"]`.

<v-click>

<div class="mt-8 text-center text-xl">

Is that a bug in the **code** or in the **test**?

</div>

</v-click>

<v-click>

<div class="mt-4 text-center opacity-80">

What do you need to know before you can answer?

</div>

</v-click>

---
layout: section
---

# Test doubles

---
layout: default
---

# `OrderService` calls a payment gateway

<v-clicks>

- A unit test must not charge a real card
- It must not be slow
- It must be able to produce a **declined** payment on demand

</v-clicks>

<v-click>

<div class="mt-8 text-center text-xl">

A real gateway will not decline on request.

So you hand it a stand-in.

</div>

</v-click>

---
layout: default
---

# The five words people argue about

| | What it is | Example |
|---|---|---|
| **Dummy** | passed to fill a signature, never used | an `EmailSender` in a test that never approves |
| **Stub** | returns canned answers | a gateway that always approves |
| **Fake** | works, but not in production | an in-memory repository |
| **Mock** | records calls so you can assert on them | a gateway you later ask *"were you called?"* |
| **Spy** | a real object, partly watched | a real repository you also observe |

---
layout: statement
---

# The distinction that matters

A **stub** sets up the situation.

A **mock** is what you assert on.

Most doubles are both — which is why nobody is careful about the words.

---
layout: default
---

# You have hand-written these already

```java
class FakePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Order order) {
        return new Approved("TX-1");
    }
}
```

<v-click>

<div class="mt-6 text-center text-green-300">

You called it a **fake** two sessions ago. By the table, it is a **stub**.

</div>

</v-click>

<v-click>

<div class="mt-3 text-center text-sm opacity-80">

Session 1's `InMemoryOrderRepository` is the real fake — a `List` where the database should be.

</div>

</v-click>

---
layout: default
---

# Where it stops scaling

<v-clicks>

- You need **three** variants, one per outcome — so you write three classes
- The interface gains a method, and every hand-written double stops compiling
- You want to assert that `sendConfirmation` **was called**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

So you add a `boolean wasCalled` field and a getter.

You are now writing a mocking library. Badly.

</div>

</v-click>

---
layout: section
---

# Mockito

---
layout: default
---

# One line

```java
PaymentGateway gateway = mock(PaymentGateway.class);
```

<v-click>

<div class="mt-8">

An object implementing the whole interface, every method returning a harmless default:

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-lg">

`null` · `0` · `false` · an empty collection

</div>

</v-click>

---
layout: default
---

# Which is a useful default

| The method returns | An unstubbed mock gives you |
|---|---|
| an object | `null` |
| `int` / `long` / `double` | `0` |
| `boolean` | `false` |
| `List` / `Set` / `Map` | an empty one, **not** `null` |
| `Optional` | `Optional.empty()` |
| `void` | nothing happens, and no error |

<v-click>

<div class="mt-4 text-center opacity-80">

So a mock you never stub is a collaborator that politely does nothing.

That is exactly what you want for the ones this test is not about.

</div>

</v-click>

---
layout: default
---

# Stubbing

```java
when(gateway.charge(order)).thenReturn(new Approved("TX-1"));
```

<v-click>

<div class="mt-6 text-center text-xl">

*When this call happens, return that.*

</div>

</v-click>

<v-click>

<div class="mt-6 opacity-80">

Anything you have not stubbed keeps returning the default — which is usually what you want.

</div>

</v-click>

---
layout: default
---

# And the thing hand-written doubles are worst at

```java
when(gateway.charge(order)).thenThrow(new GatewayTimeoutException());
```

<v-click>

<div class="mt-8 text-center text-xl">

Failure paths are exactly the ones nobody tests,

because they are the hardest to arrange for real.

</div>

</v-click>

---
layout: default
---

# Verifying

```java {all|1|2|3|4}
verify(repository).save(order);
verify(repository, times(2)).save(order);
verify(repository, never()).save(order);
verifyNoInteractions(emailSender);
```

<v-click>

<div class="mt-6">

This has no equivalent in anything you have written before.

</div>

</v-click>

---
layout: default
---

# Why it has to exist

```java {all|4|5|7}
public PaymentResult place(Order order) {
    PaymentResult result = gateway.charge(order);
    if (result instanceof Approved approved) {
        repository.save(order);
        emailSender.sendConfirmation(order, approved.transactionId());
    }
    return result;
}
```

<v-click>

<div class="mt-4">

Delete line 5. What does `place()` return now?

</div>

</v-click>

---
layout: statement
---

# Exactly the same `Approved`

No assertion on the **result** can see the difference.

---
layout: default
---

# Only this can

```java
verify(emailSender).sendConfirmation(order, "TX-1");
```

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Some behaviour **has no return value to assert on**.

That is the entire reason Mockito is in this session rather than a footnote.

</div>

</v-click>

---
layout: default
---

# Argument matchers

```java
verify(emailSender).sendConfirmation(eq(order), anyString());
```

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**One matcher means all matchers.**

Mixing a raw value with a matcher gives you `InvalidUseOfMatchersException`,

whose message is long and whose cause is always this.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-sm opacity-80">

Not needed today. Recognise the exception.

</div>

</v-click>

---
layout: default
---

# When the order genuinely matters

```java
InOrder inOrder = inOrder(repository, emailSender);

inOrder.verify(repository).save(order);
inOrder.verify(emailSender).sendConfirmation(order, "TX-1");
```

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Reach for this **rarely**.

Most of the time the order of two calls is an implementation detail, and pinning it down
means the next refactor fails a test for no reason.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-sm opacity-80">

Use it when the order is the requirement — "charge before you ship" — not when it merely
happens to be the order the code uses today.

</div>

</v-click>

---
layout: default
---

# The annotation style

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock PaymentGateway gateway;
    @Mock OrderRepository repository;
    @Mock EmailSender emailSender;
    @InjectMocks OrderService service;
}
```

<v-click>

<div class="mt-6">

Same thing, less typing. You will meet it in real codebases.

Today's exercise uses explicit `mock(...)` calls — while you are learning,
seeing the objects created is worth four extra lines.

</div>

</v-click>

---
layout: default
---

# What not to mock — 1

<div class="text-xl">

**Do not mock types you do not own.**

</div>

<v-clicks>

- Mocking a third-party client encodes *your belief* about how it behaves
- The library changes
- Your tests still pass. Production breaks

</v-clicks>

<v-click>

<div class="mt-6 text-center text-green-300">

Wrap it in your own interface, and mock that.

</div>

</v-click>

---
layout: default
---

# What not to mock — 2

<v-clicks>

- **Do not mock value objects.** `Order` is a record. Just build one
- **Do not mock everything.** Five mocks means five collaborators — the test is telling you about the *design*
- **Do not verify every interaction**

</v-clicks>

---
layout: default
---

# The over-specified suite

<div class="mt-6">

A test that asserts on all six calls fails the next time anybody refactors the class —

**without a single behaviour having changed.**

</div>

<v-click>

<div class="mt-8 text-center">

Verify what the requirement says: *the customer gets an email.*

Not the order in which three internal methods ran.

</div>

</v-click>

---
layout: statement
---

# Stub queries.

# Verify commands.

If a call *fetches* something — stub it, assert on the result.

If a call *does* something — verify it.

---
layout: default
---

# Pause

`OrderService.place` calls `gateway.charge(order)`.

You could write `verify(gateway).charge(order)` in every test.

<v-click>

<div class="mt-8 text-center text-xl">

Should you?

</div>

</v-click>

<v-click>

<div class="mt-4 text-center opacity-80">

What does it protect, given every test already stubs that call
and asserts on its result?

</div>

</v-click>

---
layout: section
---

# Judging a test suite

The uncomfortable part

---
layout: default
---

# Open the exercise. Run the tests.

<div class="mt-8 text-center text-3xl text-red-400">

Tests run: 17, Failures: 13

BUILD FAILURE

</div>

<v-click>

<div class="mt-10 text-center text-2xl">

Thirteen unwritten tests, each calling

`fail("TODO: write this test")`

</div>

</v-click>

<v-click>

<div class="mt-6 text-center opacity-80">

That is your to-do list, and it names itself.

</div>

</v-click>

---
layout: default
---

# Now delete one `fail(...)` line

<div class="mt-6 text-center text-xl">

Leave the body empty. Run again.

</div>

<v-click>

<div class="mt-8 text-center text-3xl text-green-400">

Tests run: 17, Failures: 0

BUILD SUCCESS

</div>

</v-click>

<v-click>

<div class="mt-8 text-center text-red-300">

The `fail(...)` lines are training wheels.

Real code has none — an unwritten test in the wild is simply a **green** one.

</div>

</v-click>

---
layout: statement
---

# A method with no body

throws nothing.

So it passes.

---
layout: default
---

# This is not a trick question

<div class="mt-6 text-lg">

It is the actual failure mode of test suites in the wild:

</div>

<v-clicks>

- green
- high coverage
- and nothing would have caught the bug

</v-clicks>

---
layout: statement
---

# Coverage tells you a line **ran**

It does not tell you that anything

**checked the result**.

---
layout: default
---

# So judge the suite differently

Ask: **if somebody broke this code, would a test go red?**

<v-clicks>

- Delete a guard. Swap two arguments of the same type. Remove a line that sends something
- Run the suite
- **Red** → something was genuinely checking that behaviour
- **Green** → that behaviour has no test
- Put the line back

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

whatever your coverage report says.

</div>

</v-click>

---
layout: statement
---

# A test you have never seen fail

is a test you have **no reason to trust**.

---
layout: default
---

# Try it on the empty-list guard

Delete the guard from `OrderReporter.summarise`.

<v-clicks>

- It does not crash
- It divides by zero
- It returns `NaN`, quietly

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

A report full of `NaN` travels a long way through a system before anyone notices.

The empty case is the test everybody skips. It catches the most.

</div>

</v-click>

---
layout: default
---

# Two suites, both green

| The suite | Against correct code | If you break the code |
|---|---|---|
| 13 empty bodies, `fail(...)` deleted | **17 green** | **stays green** |
| yours, finished | 17 green | **goes red** |

<v-click>

<div class="mt-6 text-center text-xl text-red-400">

Row one is the point.

</div>

</v-click>

<v-click>

<div class="mt-2 text-center">

A completely green suite that catches **nothing at all**.

</div>

</v-click>

---
layout: default
---

# Real projects automate this

It has a name — **mutation testing** — and **PIT** is the standard Java tool.

<v-click>

<div class="mt-8 text-center opacity-80">

It makes those small edits for you, dozens of them, and reports how many your suite noticed.

</div>

</v-click>

<v-click>

<div class="mt-6 border-l-4 border-yellow-500 pl-4 text-sm">

Doing it by hand once is worth more than the tool: you feel which breaks your tests miss.

</div>

</v-click>

---
layout: section
---

# Exercise

Testing the Order Service

---
layout: default
---

# 13 tests to write

Everything in `src/main/java` is **given, complete and working**. You do not change it.

<v-click>

<div class="mt-6">

| File | Tests | Really about |
|---|---|---|
| `OrderTest` | 5 | asserting on a **value**, and on a **refusal** |
| `OrderReporterTest` | 4 | asserting on a **calculation** |
| `OrderServiceTest` | 4 | asserting on a **call that happened** |

</div>

</v-click>

---
layout: default
---

# Step 0 — run it first, then finish the POM

<v-clicks>

- `mvn test` → **4 green, 13 red.** The red ones name the tests you are about to write
- Then two POM TODOs: **AssertJ** and **Mockito**
- Four lines each. The same shape you wrote last session

</v-clicks>

---
layout: default
---

# Steps 1 to 3

<v-clicks>

- **`OrderTest`** — two models given. Normalisation, two refusals, two `withDiscount` tests
- **`OrderReporterTest`** — work the numbers out **by hand**. A test that recomputes the logic tests nothing
- **`OrderServiceTest`** — the Mockito part. Saves, emails, and two "does nothing" cases

</v-clicks>

<v-click>

<div class="mt-6 border-l-4 border-yellow-500 pl-4">

The top-customer test has a trap in the data: one customer placed the biggest single order,
a **different** one spent the most in total. Read before you assume.

</div>

</v-click>

---
layout: default
---

# Step 4 — watch them fail

Break the code on purpose, three times, one at a time:

<v-clicks>

- delete the `total < 0` check in `Order`
- swap `revenue` and the average in `OrderReporter`
- delete the `sendConfirmation(...)` line in `OrderService`

</v-clicks>

<v-click>

<div class="mt-6 text-center">

Each one should turn **exactly one** test red, and its name should say what you broke.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-red-300">

Undo each change before the next. Stays green? That behaviour has no test.

</div>

</v-click>

---
layout: default
---

# Homework — three questions your suite cannot answer

Thirteen green tests, and three behaviours still unpinned.

<v-clicks>

- An **invalid discount** — every test passed a sensible percentage
- A **null** list, not an empty one — one `if`, two conditions, one test
- An order that is **legitimately free** — nobody ever built one
- Budget 45 minutes. Nobody tells you what to assert

</v-clicks>

---
layout: default
---

# The third one is the interesting one

It makes the validation **stricter**.

<v-clicks>

- Every existing test still passes
- Because none of them ever built a legitimate free order

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

A suite that only pins down what is **forbidden**

lets somebody tighten a rule and break a real customer.

</div>

</v-click>

---
layout: section
---

# Recap

---
layout: default
---

# Where we started

<v-clicks>

- **`Optional`** is a box that may be empty — a return type that cannot lie about it
- `orElseGet` over `orElse` when the fallback costs something
- `ifPresent` / `map` over `isPresent()` + `get()`
- Return types only — never a field, a parameter, or a collection
- A **test** runs your code and reaches a **verdict**
- **Units** name what broke; integration and end-to-end prove more and say less

</v-clicks>

---
layout: default
---

# The tools

<v-clicks>

- Your old harness could not **isolate**, **name**, **set up** or **report**. That is most of what JUnit is
- Each `@Test` gets a **fresh instance** — that is where independence comes from
- `@DisplayName` + `@Nested` turn a suite into an outline of what the class promises
- **AssertJ** puts actual first and tells you what went wrong. `within(...)` on every double
- **Mockito**: `mock`, `when(...).thenReturn(...)`, `verify`. `verify` exists because some behaviour has no return value

</v-clicks>

---
layout: default
---

# The judgement

<v-clicks>

- **Arrange, Act, Assert.** One behaviour per test. A name that reads as a sentence
- **Stub queries, verify commands**
- Do not mock what you do not own; do not mock value objects; do not verify everything
- An over-specified suite fails on every refactor and protects nothing
- **Green is not the pass condition.** An empty test passes
- A suite is worth exactly what it catches

</v-clicks>

---
layout: statement
---

# Next session

**The Spring container**

where dependency injection stops being a discipline you follow

and becomes something a framework does for you.
