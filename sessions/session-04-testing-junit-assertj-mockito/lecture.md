# Testing in Java

*A catch-up on `Optional`, then JUnit 5, AssertJ, and Mockito.*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause** prompts and actually answer them before reading on.*

**Before you start**

- You need the previous session done: **Maven**, and a project you can open from a `pom.xml`. Today's three libraries arrive as three dependency blocks, and you will add two of them yourself.
- **Network access on the first build**, again. AssertJ and Mockito have to come down from Maven Central once.
- JDK 25, IntelliJ.

**By the end you'll be able to:**

- Use **`Optional`** — `orElse`, `orElseGet`, `map`, `filter` — and say what `null` cost you that it does not.
- Say what a **test** is, name the **three kinds**, and say why today is entirely units.
- Say what a unit test is *for*, in terms of the harness you have already been using and its four specific failures.
- Write a JUnit 5 test: `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested`, and a name that reads as a sentence.
- Structure every test as **Arrange, Act, Assert**, and name the five ways that shape goes wrong.
- Assert with **AssertJ** — values, collections, exceptions — and say why its failure messages are the actual reason people switch.
- Explain the difference between a **stub** and a **mock**, and why "test double" is the honest umbrella word.
- Use **Mockito**: `mock`, `when(...).thenReturn(...)`, `verify`, `never()`, `verifyNoInteractions`.
- Say what you should **not** mock, and what a suite of over-specified mocks costs you.
- Judge a test suite by whether it **catches a broken change**, not by whether it is green.

---

## `Optional`

Last session ended on `findFirst()`, and it did not hand you an `Employee`:

```java
Optional<Employee> first = staff.stream()
        .filter(e -> e.salary() > 100_000)
        .findFirst();
```

There may be nobody earning that much. `findFirst` has to answer that honestly, so it returns a box that may or may not contain an employee. We ran out of time to say why that is an improvement, so we start there.

### What `null` costs

Read this signature and answer the question:

```java
Employee findByName(String name)
```

Can it return `null`? The signature does not say. You find out from the javadoc if somebody wrote one, from the body if you have the source, or from production if you are unlucky. And when you forget the check, the `NullPointerException` does not fire here — it fires wherever the value is finally used, which may be three classes and two hours away from the method that decided to return nothing.

The root of it is that `null` is a value meaning "no value", and it is assignable to every reference type. That is precisely why the compiler cannot help: it has no way to tell the `Employee` that exists from the `Employee` that does not.

`Optional<Employee>` moves that fact into the type, where the compiler can see it:

```java
Optional<Employee> findByName(String name)
```

Same code, same behaviour. The difference is that the caller now *cannot* forget, because there is no `Employee` to use until they have said what should happen when there isn't one.

### Making one

```java
Optional.of(employee);           // NPE immediately if employee is null
Optional.ofNullable(maybeNull);  // empty if it happens to be null
Optional.empty();                // deliberately nothing
```

`of` is the strict one, and that is the point: reach for it when `null` would be a bug, so it fails at the place the bug is rather than later. Most of the time, though, you do not construct these at all. You receive them — from `findFirst`, from `max`, from `average`, and from any API written since Java 8.

### Getting the value out

| Call | What you get |
|---|---|
| `orElse(other)` | the value, or `other` |
| `orElseGet(supplier)` | the value, or a fallback computed only if it is needed |
| `orElseThrow()` | the value, or `NoSuchElementException` |
| `orElseThrow(supplier)` | the value, or an exception you chose |
| `ifPresent(action)` | runs the action only if there is a value |
| `ifPresentOrElse(action, empty)` | one branch each way |
| `map(fn)` | the value transformed, still in a box |
| `filter(predicate)` | the value if it also matches, otherwise empty |
| `isPresent()` / `isEmpty()` | a plain `boolean` |

**`orElse` and `orElseGet` are not interchangeable**, and this is the one that bites people:

```java
Employee a = found.orElse(expensiveDefault());
Employee b = found.orElseGet(() -> expensiveDefault());
```

`orElse` takes a *value*, so Java evaluates the argument before the call is made — `expensiveDefault()` runs every time, including when `found` has something in it and the result is thrown straight away. `orElseGet` takes a lambda, so the body runs only when the box is empty. For a constant, `orElse` is fine and reads better. For anything that costs — a query, a large object, a call out to another service — use `orElseGet`.

### The rule: do not unwrap it by hand

```java
if (found.isPresent()) {
    use(found.get());
}
```

That is a null check with extra ceremony. You accepted the wrapper and then immediately undid it, so you carry all of the noise and gain none of the safety — including the `NoSuchElementException` that is waiting for the day somebody edits the `if`. Write this instead:

```java
found.ifPresent(this::use);
```

`get()` is not forbidden. But every unguarded `get()` is a place where the type system told you something and you overruled it, so it should be rare enough to notice.

### It behaves like a stream of nought or one

`map` and `filter` mean exactly what they meant on a stream, over a collection that holds at most one thing:

```java
found.map(Employee::department)
     .filter(d -> d.equals("Engineering"))
     .ifPresent(System.out::println);
```

If `found` is empty, nothing in that chain runs and nothing throws. That is the payoff: "is there anything?" gets asked once, by the library, instead of at every step by you.

### Where it belongs

`Optional` is a **return type**. That is the job it was designed for, and it is the only one it is good at.

- **Not fields.** It is an extra object on every instance, and it is not serialisable. A field that may be absent is just a nullable field — guard it in the constructor.
- **Not parameters.** The caller has to wrap a value they already hold. Write two overloads instead.
- **Not collections.** An empty `List` already means "nothing found". `Optional<List<Employee>>` hands the caller two ways to say the same thing, and eventually two bits of code will disagree about which one to check.

> **Pause.** A method looks up an employee by id, and the id always comes from a dropdown the user picked from — so it is always found. Should it return `Optional<Employee>` or `Employee`? Now answer again for the day somebody calls that method from a batch script, with ids read out of a CSV.

**Play with it.** Nothing in today's exercise uses `Optional`, and nothing checks whether you tried any of this. Make yourself a scratch class in the project, put five employees in a list, and look at what `findFirst`, `max`, `filter().findAny()` and `average()` actually hand back. Then call `orElse` and `orElseGet` with a method that prints a line when it runs, and watch which one prints. Delete it afterwards or keep it — it is yours.

---

## You have already written a test framework

For two sessions, every exercise shipped a `Verify.java`. You ran it, it printed `PASS` and `FAIL` lines and a count, and when it said `ALL 24 CHECKS PASSED` you believed it — correctly.

That was a test framework. A small one, hand-rolled, and good enough to learn with. It has exactly four problems, and JUnit is what those four problems look like once somebody has solved them properly.

**One: no isolation.** Your checks run one after another in a single `main`. One check that *throws* — not fails, throws — takes the rest of the run with it. The checks below it never execute, and their status is unknown. You cannot tell "one thing is broken" from "everything after line 40 is unknown".

**Two: no names.** `check 7` is not a sentence. When it fails six months from now, you read the code of check 7 to find out what the program was supposed to do. A name like *"rejects an order with a negative total"* is documentation that cannot go out of date, because it fails when it stops being true.

**Three: no setup.** Every check builds its own fixtures by hand, so the same four lines appear eleven times, and when the constructor changes you edit eleven places.

**Four: no report.** It prints to a console. A build server needs a *file* — which tests ran, which failed, how long they took — so it can tell you that this test started failing on Tuesday's commit. Console output is not a record.

> **Pause.** Before reading on: which of the four problems above is the one that would actually let a bug reach production? Argue for one. (There is a defensible answer for at least two of them.)

---

## What a test is

A **test** is code that runs your code and decides whether the answer was right.

Three steps, always: call the thing, state what you expected, and fail loudly when the two do not match. That last word is what separates a test from a script. A program that prints numbers for you to squint at is a *report* — it still needs a human to reach the verdict. A test reaches the verdict itself, which is why a machine can run ten thousand of them while you are at lunch.

Why write them? The obvious answer is "to check the code works". The real answer is the second one: **to check that it still works after you change it**. Code nobody edits barely needs tests. Code that ten people edit over three years needs them badly, because without them *"I refactored it"* and *"I broke it"* produce exactly the same silence, until a customer finds out.

### The three kinds

| | What it covers | How fast | How many you write |
|---|---|---|---|
| **Unit** | one class, with stand-ins for everything it talks to | milliseconds | thousands |
| **Integration** | your code plus one real thing — a database, a message broker | seconds | hundreds |
| **End-to-end** | the whole system, driven through its front door | minutes | a handful |

Each row proves more than the row above it. Each row also runs slower, breaks for more reasons that are not your fault, and tells you less about *what* went wrong. An end-to-end failure says "something, somewhere, is wrong". A unit failure names the class and the behaviour.

So a healthy suite is shaped like a pyramid: a wide base of unit tests that run on every save, a narrower band of integration tests on every push, and a few end-to-end tests guarding the journeys that must never break. Inverting that — a handful of units under a mountain of slow browser tests — is a well-travelled route to a suite nobody runs.

You meet integration tests in the persistence course, and again in the CI session. Today is entirely units.

---

## What a unit test is

A **unit test** exercises one small piece of behaviour, in isolation, and asserts on the result. Fast — milliseconds — because it touches no network, no database, no filesystem and no clock.

Three properties that matter more than the definition:

- **Deterministic.** Same input, same result, every time, on every machine. A test that fails one run in twenty is worse than no test, because the team learns to re-run it.
- **Independent.** Order does not matter. Test 4 does not depend on test 3 having run.
- **A sentence.** The name says what the code should do. The body proves it.

And the shape, every time: **Arrange, Act, Assert** — three blocks, blank lines between them. It gets a section of its own below, once JUnit's annotations are on the table and the example can be a whole test method.

**One behaviour per test.** Not one method — one behaviour. `place()` deserves four tests, because it does four distinguishable things. That is not pedantry: when a test fails, its name should tell you what broke without opening it.

> **Pause.** Which of these is a unit test? (1) builds an `Order` and checks the total; (2) starts an in-memory database and saves a row; (3) calls the real payment provider's sandbox; (4) runs `OrderReporter` over a list of five orders. Two are clear and one is not — and the arguable one is arguable for a reason worth naming.

---

## JUnit 5

Three dependency lines, and you have all of it:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.1</version>
    <scope>test</scope>
</dependency>
```

You wrote exactly this last session. That is worth noticing: a testing library is not a special kind of thing. It is a dependency with `test` scope, and it stays out of the jar you ship.

### The annotations you need today

```java
class OrderTest {

    @BeforeEach
    void setUp() { ... }          // before every test

    @Test
    @DisplayName("rejects an order with a negative total")
    void rejectsNegativeTotal() { ... }
}
```

| | |
|---|---|
| `@Test` | this method is a test |
| `@DisplayName("...")` | what shows in the report. A sentence, not an identifier |
| `@BeforeEach` / `@AfterEach` | run before / after **every** test |
| `@BeforeAll` / `@AfterAll` | run once for the class. Must be `static`. Use sparingly — shared state between tests is how independence dies |
| `@Nested` | an inner class grouping related tests |
| `@Disabled("why")` | skipped. Always give the reason |

**Each test gets a brand-new instance of the test class.** JUnit constructs the class again for every `@Test`. That is where independence comes from — a field you set in test 1 does not exist in test 2. It also means `@BeforeEach` is not an optimisation; it is how each test gets its fixtures.

**Test classes and methods do not need to be `public`.** Package-private is the convention, and it is why your test files have no `public` on the class.

### Arrange, Act, Assert

Every test today has the same three blocks, in the same order, with a blank line between them. The pattern is decades old and the name is the whole explanation: you **arrange** a situation, you **act** on it, and you **assert** what should now be true.

```java
@Test
@DisplayName("takes a percentage off the total")
void appliesDiscount() {
    Order order = new Order("chan@example.com", 200.00, 1);   // Arrange

    Order discounted = order.withDiscount(10);                // Act

    assertEquals(180.00, discounted.total(), 0.001);          // Assert
}
```

Those are JUnit's own assertions — `assertEquals` from `org.junit.jupiter.api.Assertions`, nothing else added yet. The third argument is a tolerance, and why a `double` needs one is a subject of its own, a few pages down. AssertJ arrives later in this session and rewrites the Assert line; the three blocks do not change when it does.

The comments are there because this is a lecture. In real tests the blank lines do that job — three paragraphs, and a reader knows which is which without being told.

| Block | The question it answers | What lives in it |
|---|---|---|
| **Arrange** | *Given what?* | the object under test, its inputs, its stubbed collaborators |
| **Act** | *When what happens?* | exactly one call — the behaviour the test is named after |
| **Assert** | *Then what must be true?* | expectations about what Act produced, and nothing else |

**Arrange — build the world this behaviour needs, and nothing more.** Construct the object under test, its inputs, and any stand-ins it talks to. Every value is fixed and written in the test: no clock, no random, no "whatever is in the database". A reader must be able to predict the expected answer from this block alone. Two things it tells you when it goes wrong: if Arrange is fifteen lines, the class is hard to construct and that is design feedback, not a testing problem; if the same Arrange appears in every test of the class, it belongs in `@BeforeEach`.

**Act — call the one thing under test, once.** One line, and where there is a return value, capture it in a variable. That line is what the `@DisplayName` describes. If you need two calls, one of them is really Arrange — putting the object into a starting state — or you are testing two behaviours and owe yourself two tests. When the method returns `void`, Act is still one line; the evidence just moves to the Assert block, where you check the state it changed or the collaborator it called.

**Assert — state what must now be true, and fail if it is not.** Assertions talk about what Act produced. Never call the code under test again here: a second call is a second Act, and when the test goes red you cannot tell which call broke. Keep every assertion in one test about one behaviour — a test with assertions about the total, the email *and* the audit log has three reasons to fail and one name.

**Given, When, Then** is the same three blocks with BDD vocabulary. You will meet it in Spring's documentation and in Mockito's `given(...).willReturn(...)`. Same shape, different words; do not let anyone tell you they are two patterns.

Two cases where the blocks look different, and both show up today:

- **Exception tests fuse Act and Assert.** The call has to happen inside the assertion, because it throws instead of returning:

  ```java
  Order order = new Order("chan@example.com", 200.00, 1);      // Arrange

  assertThrows(IllegalArgumentException.class,                 // Act + Assert
          () -> order.withDiscount(150));
  ```

  The call goes inside the lambda. Outside it, the exception escapes the test method and the test errors instead of passing — the single commonest mistake in the exercise, in JUnit's form and in AssertJ's.

- **Mock-based tests move weight into Arrange and Assert.** The stubbing — `when(gateway.charge(order)).thenReturn(new Approved("TX-1"))` — is Arrange: it is part of the situation. The `verify(repository).save(order)` is Assert: it is an expectation, just about a call that happened rather than a value that came back. Act is still the single `service.place(order)` in the middle.

Five ways it goes wrong, all of which you will see in your own tests before the session ends:

| Smell | Why it hurts |
|---|---|
| An assertion in the Arrange block | you are testing the fixture; if it can break, it deserves its own test |
| Two Act lines | the failure no longer names one behaviour |
| No Act at all | asserting on the object you just arranged tests the constructor, whatever the name says |
| The call repeated inside Assert | two executions; red tells you nothing about which |
| Arrange reads a clock, a random or a file | the test is no longer deterministic, and one run in twenty will lie |

> **Pause.** `savesOnApproval` needs a stubbed gateway, a `place()` call and a `verify` on the repository. Write the three lines in your head and label each block. Then say which block the *order* `new Order(...)` belongs in — and why the answer does not change when the test is about the email instead.

### `@Nested`, and why it is worth the extra braces

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

The report then reads as an outline:

```
Order
  when it is built
    ✔ keeps the values it was given
    ✔ trims and lower-cases the email
  when the arguments are wrong
    ✔ rejects a negative total
```

That is a specification of the class, generated from tests that are currently true. It is the closest thing to documentation that cannot rot.

> **Pause.** `@BeforeAll` is `static` and runs once. `@BeforeEach` runs every time. Name a fixture that genuinely belongs in `@BeforeAll` — and then say what could go wrong if a test modified it.

---

## AssertJ

JUnit ships assertions of its own — the ones in the three-block example above:

```java
assertEquals(180.00, discounted.total(), 0.001);
```

They work. Two things about them wear badly.

**The argument order.** Is it `assertEquals(expected, actual)` or `assertEquals(actual, expected)`? It is expected-first, and roughly half of all Java developers get it backwards at least once a year. When they do, the test still passes or fails correctly — but the failure message has expected and actual the wrong way round, and you spend ten minutes debugging the wrong side.

**The failure messages**, which are the real reason. AssertJ reads left to right — actual first, always — and its failures tell you what happened rather than printing two values:

```java
import static org.assertj.core.api.Assertions.assertThat;

assertThat(discounted.total()).isEqualTo(180.00, within(0.001));
```

### The assertions you need today

```java
// values
assertThat(order.customerEmail()).isEqualTo("chan@example.com");
assertThat(order.total()).isEqualTo(180.00, within(0.001));
assertThat(report.orderCount()).isEqualTo(3);
assertThat(result).isInstanceOf(Approved.class);
assertThat(result).isNotNull();

// strings
assertThat(message).contains("negative");

// collections
assertThat(orders).hasSize(3);
assertThat(names).containsExactly("Alice", "Bob");         // order matters
assertThat(names).containsExactlyInAnyOrder("Bob", "Alice");
assertThat(orders).extracting(Order::customerEmail).contains("chan@example.com");
assertThat(orders).isEmpty();

// exceptions
assertThatThrownBy(() -> new Order("nope", 10.00, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("customerEmail");
```

Three of those are worth dwelling on.

**`within(...)` on doubles, always.** This is the tolerance the JUnit example left unexplained, and it is the same idea in both libraries: JUnit takes it as a third argument to `assertEquals`, AssertJ takes it as `within(...)`, and either way the assertion passes when actual and expected differ by no more than the amount you named.

Why any of it is needed: a `double` is stored in binary, and most decimal fractions have no exact binary form, so `0.1 + 0.2` is not `0.3` and a ten-percent discount off two hundred hands you `180.00000000000003`. An exact-equality assertion fails on that, correctly — the two numbers really are different. This is not the library being fussy; it is arithmetic, and a tolerance is how you say "equal to the precision I care about". Pick one that is smaller than any difference that would matter: `0.001` on money means a tenth of a cent, which no real discount bug hides beneath. Money in a real system belongs in `BigDecimal` — a subject for the persistence course — but the habit of a tolerance starts today.

**`assertThatThrownBy` takes a lambda**, because the exception has to be thrown *inside* something AssertJ can catch — the same reason JUnit's `assertThrows` takes one, and it replaces `assertThrows` in the exercise because it can go on to assert about the message. The commonest mistake is calling the method outside the lambda, where the exception escapes the test and it errors rather than passing.

**`extracting(...)`** turns a list of objects into a list of one field, so you can assert on the interesting part instead of writing `equals` for a whole object.

### Where AssertJ actually pays

Consider a list assertion that fails.

`assertEquals` gives you two lists printed end to end and leaves you to find the difference. AssertJ tells you which elements were missing, which were unexpected, and in what position — and for a record with six components, it names the component that differs instead of printing two long `toString`s.

That is the whole pitch. It is not a nicer syntax; it is the ten minutes you do not spend reading two lines of output that differ in one character.

> **Pause.** Your test asserts `assertThat(names).containsExactly("Alice", "Bob")` and it fails saying the actual list was `["Bob", "Alice"]`. Is that a bug in the code or in the test? What do you need to know before you can answer?

---

## Test doubles

`OrderService` calls a payment gateway. A unit test must not charge a real card, and it must not be slow, and it must be able to produce a *declined* payment on demand — which a real gateway will not do for you.

So you hand it a stand-in. The umbrella word is **test double**, and the varieties are worth knowing by name because people use them interchangeably and then argue:

| | What it is | Example |
|---|---|---|
| **Dummy** | passed to satisfy a signature, never used | an `EmailSender` in a test that never approves anything |
| **Stub** | returns canned answers | a gateway that always returns `Approved` |
| **Fake** | a real, working, unsuitable-for-production implementation | an in-memory `OrderRepository` backed by a `HashMap` |
| **Mock** | records what happened, so you can assert on the calls | a gateway you later ask *"were you called, with what?"* |
| **Spy** | a real object, with some calls recorded or overridden | a real repository you also want to watch |

The distinction that matters: a **stub** helps you set up the situation; a **mock** is what you *assert on*. Most test doubles are both, which is why nobody is careful about the words.

### You have already hand-written these

```java
class FakePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult charge(Order order) {
        return new Approved("TX-1");
    }
}
```

You wrote that class two sessions ago and called it a *fake*. By the table above it is a **stub**: it returns canned answers and records nothing. Session 1's `InMemoryOrderRepository` is the genuine fake — a working repository with a `List` where the database should be. Nobody will correct you in a code review, and that is the honest state of the vocabulary; know what the five words mean and stop worrying about which one is on the class.

There is nothing wrong with the stub itself. It stops scaling when:

- you need three variants of it, one per outcome, so you write three classes;
- the interface gains a method and every hand-written double stops compiling;
- and — the real one — you want to assert that `sendConfirmation` **was called**, which means adding a `boolean wasCalled` field and a getter, and now you are writing a mocking library, badly.

---

## Mockito

```java
PaymentGateway gateway = mock(PaymentGateway.class);
```

One line, and you have an object implementing the whole interface, every method returning a harmless default: `null`, `0`, `false`, an empty collection.

### Stubbing

```java
when(gateway.charge(order)).thenReturn(new Approved("TX-1"));
```

Read it as: *when this call happens, return that.* Any call that has not been stubbed keeps returning the default, which is usually what you want.

```java
when(gateway.charge(order)).thenThrow(new GatewayTimeoutException());
```

Throwing on demand is the thing hand-written doubles are worst at, and the failure paths are exactly the ones nobody tests.

### Verifying

```java
verify(repository).save(order);                    // exactly once, with that order
verify(repository, times(2)).save(order);
verify(repository, never()).save(order);
verifyNoInteractions(emailSender);                 // nothing at all was called on it
```

This is the piece that has no equivalent in anything you have written before, and it exists because **some behaviour has no return value to assert on**.

Look at `OrderService.place`. On an approved payment it saves the order and emails the customer, and then returns the same `Approved` it would have returned anyway. Delete the email line and `place()` still returns exactly the same value. No assertion on the result can see the difference. Only `verify(emailSender).sendConfirmation(order, "TX-1")` can.

Deleting that one line is the kind of break no assertion on a return value can see, and it is why Mockito is in this session rather than being a footnote.

### Argument matchers, briefly

When you do not care about an argument, or cannot predict it:

```java
verify(emailSender).sendConfirmation(eq(order), anyString());
```

The rule that trips everyone: **if you use a matcher for one argument, you must use one for all of them.** `eq(order)` is how you say "this exact value" in matcher form. Mixing a raw value with a matcher gives you `InvalidUseOfMatchersException`, whose message is long and whose cause is always this.

You will not need matchers in today's exercise. Recognise the exception.

### The annotation style

You will meet this in real codebases:

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock PaymentGateway gateway;
    @Mock OrderRepository repository;
    @Mock EmailSender emailSender;
    @InjectMocks OrderService service;
}
```

Same thing, less typing, and `@InjectMocks` finds the constructor and wires them. Today's exercise uses the explicit `mock(...)` calls instead, deliberately — while you are learning, seeing the objects created is worth four extra lines. Both are correct.

### What not to mock

This is the part that separates a suite that helps from one that hurts.

- **Do not mock types you do not own.** Mocking a third-party client encodes *your belief* about how that library behaves. When the library changes, your tests still pass and production breaks. Wrap it in your own interface and mock that.
- **Do not mock value objects.** `Order` is a record. Just build one. A mocked record is slower to write, harder to read, and lies about what it can do.
- **Do not mock everything.** If a class needs five mocks, the class has five collaborators, and the test is telling you something about the design rather than about the code.
- **Do not verify every interaction.** A test that asserts on all six calls fails the next time anybody refactors the class, without a single behaviour having changed. Verify what the *requirement* says: the customer gets an email. Not the order in which three internal methods ran.

The rule of thumb: **stub queries, verify commands.** If a call fetches something, stub it and assert on the result. If a call *does* something — saves, sends, publishes — verify it.

> **Pause.** `OrderService.place` calls `gateway.charge(order)` and you could write `verify(gateway).charge(order)` in every test. Should you? What does that assertion actually protect, given every test already stubs that call and asserts on its result?

---

## Judging a test suite

Here is the uncomfortable part of the session.

Open today's exercise and run the tests before writing anything. **Seventeen tests, thirteen failures.** Every test you have not written yet calls `fail("TODO: write this test")`, so the suite is red on purpose and the red list is your to-do list.

Now do the thirty-second experiment that is the actual point. Delete that one `fail(...)` line from `normalisesTheEmail`, leaving the body empty, and run again: **seventeen tests, zero failures, BUILD SUCCESS.** A method with no body throws nothing, so it passes. Put the line back.

Those `fail(...)` lines are training wheels. Real code has none — an unwritten test in the wild is simply a green one.

That is not a trick. It is the actual failure mode of test suites in the wild: green, high coverage, and nothing would have caught the bug. **Coverage tells you a line ran. It does not tell you anything checked the result.**

### So how do you judge one?

Not by the bar, and not by the coverage percentage. Ask the only question that matters: **if somebody broke this code, would a test go red?**

That question has a concrete form you can run on any suite, including your own, in a minute:

- Open the production file. Delete a guard clause, swap two arguments of the same type, or remove a line that sends something.
- Run the suite.
- **Red** → some test was genuinely checking that behaviour. **Green** → that behaviour has no test, whatever the coverage report says.
- Put the line back.

Try it on `OrderReporter.summarise` once your tests are written. Remove the empty-list guard: the code does not crash — it divides by zero, returns `NaN` quietly, and a report full of `NaN` travels a long way through a system before anybody notices. If your suite stays green, the empty case was never tested, and the empty case is the one everybody skips.

The habit is worth more than any tool: **a test you have never seen fail is a test you have no reason to trust.** Write it, watch it go red, then make it green.

---

## Exercise — Testing the Order Service

**13 tests to write.** Everything under `src/main/java` is given, complete and working; you do not change it.

### Step 0 — run the suite, then finish the POM (~10 min)

Run `mvn test` first: **four green, thirteen red**. The red ones are the tests you are about to write, and they name themselves. Then delete one `fail(...)` line, run again, and watch that test pass with an empty body — then put it back. Never trust a green bar again on its own.

Then two POM `TODO`s: AssertJ and Mockito. Four lines each, the same shape as last session.

### Step 1 — `OrderTest`, five tests (~12 min)

Two are written as models — one `assertThat`, one `assertThatThrownBy`. Write the other five: normalisation, two more validation refusals, and the two `withDiscount` tests. The second discount test is the one that proves the record is immutable rather than assuming it.

### Step 2 — `OrderReporterTest`, four tests (~12 min)

Work the expected numbers out **by hand** from the sample orders and write them as literals. A test that recomputes the production logic tests nothing.

The top-customer test has a deliberate trap in the data: one customer placed the single largest order, a different one spent the most in total. Read before you assume.

### Step 3 — `OrderServiceTest`, four tests (~15 min)

The Mockito part, and the point of the session. Three mocks and one test are given. Write: saves on approval, emails on approval with the gateway's transaction id, and the two "does nothing" tests for `Declined` and `OutOfStock`.

### Step 4 — watch them fail (~6 min)

A green suite you have never seen red proves nothing. For three of your thirteen tests, break the production code on purpose — delete the negative-total check in `Order`, swap `revenue` and the average in `OrderReporter`, delete the `sendConfirmation` line in `OrderService` — and run the suite each time.

Each break should turn exactly one test red, and the name of that test should tell you what you broke. **Undo each change before the next one.** If a break leaves the suite green, that behaviour has no test yet, whatever the file looks like.

### If you get stuck

| What you see | What it means |
|---|---|
| `package org.assertj.core.api does not exist` | POM TODO 1 |
| `package org.mockito does not exist` | POM TODO 2 |
| A test passes that you have not written | You deleted its `fail(...)` without writing a body. An empty test passes — that is the lesson |
| `expected <180.0> but was <180.00000000000003>` | Use `within(0.001)` |
| `Expecting code to raise a throwable, but nothing was thrown` | The call is outside your `assertThatThrownBy` lambda |
| `Wanted but not invoked` | `verify` says it never happened — check which result you stubbed |
| `UnnecessaryStubbingException` | You stubbed a call the code never makes. Usually a copy-paste from the test above |
| `InvalidUseOfMatchersException` | One argument is a matcher and another is a raw value. Wrap the raw one in `eq(...)` |

---

## Homework — three questions your suite cannot answer

Same project, no new files. Finish the thirteen tests and your suite is green — and there are still three behaviours nobody has pinned down. Write one test for each.

1. **An invalid discount.** `withDiscount` refuses a percentage below 0 or above 100. Every test you wrote passed it a sensible number, so delete that guard and the suite stays green. What does `withDiscount(150)` produce if nobody checks?
2. **A null list, not an empty one.** `summarise` guards `null` *and* empty, and you tested empty. Those are different branches and only one of them has a test.
3. **An order that is legitimately free.** Change `total < 0` to `total <= 0` in `Order` and every test still passes, because none of them ever built a free order — a replacement, a fully-discounted line. This one is the interesting case: the rule got **stricter**, not broken, and your suite cannot tell the difference between a deliberate rule change and somebody cutting off a real customer.

Budget 45 minutes. The third is the point of the set: a suite that only pins down what is **forbidden** never notices when what is **allowed** quietly shrinks.

---

## Recap

- **`Optional`** is a return type that admits there may be nothing. `orElseGet` over `orElse` when the fallback costs something, `ifPresent`/`map` over `isPresent()` + `get()`, and never on a field, a parameter or a collection.
- A **test** runs your code and reaches a verdict. Units name what broke, integration and end-to-end prove more and say less — so the base of the pyramid is wide.
- You had already written a test framework. It could not **isolate**, **name**, **set up** or **report** — and those four are most of what JUnit is.
- Each `@Test` gets a **fresh instance** of the class. That is where independence comes from.
- `@DisplayName` and `@Nested` turn a suite into an outline of what the class promises.
- **Arrange, Act, Assert**, one behaviour per test, and a name that reads as a sentence.
- **AssertJ** puts actual first and tells you what went wrong. Use `within(...)` on every double, and `assertThatThrownBy` for refusals.
- A **test double** stands in for a collaborator. Stub the queries, verify the commands.
- **Mockito**: `mock`, `when(...).thenReturn(...)`, `verify`, `never()`, `verifyNoInteractions`. `verify` exists because some behaviour has no return value.
- **Do not mock what you do not own**, do not mock value objects, and do not verify every interaction — an over-specified suite fails on every refactor and protects nothing.
- **Green is not the pass condition.** An empty test passes. A suite is worth what it catches — break the code on purpose and see whether anything goes red.

---

## Glossary

| Term | Meaning |
|---|---|
| **`Optional`** | a box holding a value or nothing, so the type says which |
| **`orElseGet`** | a fallback computed only when the box is empty |
| **Test** | code that runs your code and reaches a verdict on the result |
| **Unit test** | one behaviour, in isolation, in milliseconds |
| **Integration test** | your code plus one real dependency — a database, a broker |
| **End-to-end test** | the whole system, driven through its front door |
| **Test pyramid** | many units, fewer integration tests, a handful end-to-end |
| **Fixture** | the objects a test needs before it can act |
| **Arrange / Act / Assert** | the three blocks of a readable test |
| **Test double** | any stand-in for a real collaborator |
| **Stub** | a double that returns canned answers |
| **Fake** | a working implementation unsuitable for production |
| **Mock** | a double that records calls so you can assert on them |
| **Spy** | a real object with some calls recorded or overridden |
| **Stubbing** | `when(...).thenReturn(...)` |
| **Verification** | `verify(...)` — asserting a call happened |
| **Argument matcher** | `any()`, `eq(...)` — "any argument", "this one" |
| **Surefire** | the Maven plugin that runs tests and writes the report |
| **Coverage** | how much code ran during the tests. Not how much was checked |

---

## Take it home

- Find the largest method in something you have written and try to test it. Notice how much setting up it needs. That difficulty is a measurement of the design, not of your testing skill — and it is the argument for constructor injection you first met in the OOP session.
- Look up **mutation testing** and the tool **PIT**. It automates the break-it-on-purpose check: it edits your code in dozens of small ways and reports how many of those edits your suite noticed.
- Read a test file in a project you did not write. Ask whether you could reconstruct what the class promises from the test names alone. If not, that is what `@DisplayName` is for.

---

## Looking ahead

Next session is **the Spring container**, and dependency injection stops being a discipline you follow by hand and becomes something a framework does for you. Everything you learned about test doubles today is what makes that framework testable: Spring can hand your class a mock as easily as the real thing, because your class never decides which it gets.

Module 1 ends here. Modern Java is behind you, the build tool works, and you can write a test that fails for the right reason — which is the whole entry requirement for the rest of the course.
