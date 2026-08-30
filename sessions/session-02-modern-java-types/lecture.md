# Modern Java Types

*`var`, records, sealed classes and interfaces, text blocks.*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause** prompts and actually answer them before reading on.*

**Before you start**

- Complete the previous session. You will refactor the `Order` class you used there.
- You need **JDK 25**. Records need 16+, sealed types need 17+. On JDK 11 half of this session will not compile. Check with `java -version` **and** `javac -version` — both must print 25.
- You will edit in IntelliJ and run from a terminal. IntelliJ's built-in terminal (**View → Tool Windows → Terminal**, or **Alt+F12**) keeps you in one window.

**By the end you'll be able to:**

- Use `var` where it improves readability and refuse it where it hurts — and name the one place it silently loses you type safety.
- Replace a data-carrier class with a **record**, and say exactly what the compiler generated in its place.
- Write a **compact constructor** that validates *and* normalises, so an invalid object cannot exist.
- Say where a record's immutability stops, and close that hole in one line.
- Model a closed set of outcomes as a **sealed interface**, and explain what the compiler can now prove that it could not prove before.
- Use **text blocks** for SQL and JSON without escaping a single quote, and predict where the spaces and newlines go.
- Say why Spring codebases are full of all four.

Four features today. We start with the smallest one — `var` — because it turns up in every example that follows.

---

## `var` — local-variable type inference

Since Java 10 you may replace a local variable's *declared type* with `var`:

```java
var orders = new ArrayList<Order>();          // an ArrayList<Order>
var total = 0.0;                              // a double
var name = "Alice";                           // a String
```

The type has not gone anywhere. The compiler works it out from the right-hand side, writes it into the class file, and fixes it forever. `name` is a `String` and always will be — the line `name = 42;` does not compile.

This matters enough to say twice: **this is not JavaScript's `var`, and it is not dynamic typing.** Nothing about your program becomes looser at runtime. The only thing that changed is how much you had to type.

### Where `var` helps

When the type appears twice on one line, `var` removes the duplicate:

```java
// before
Map<String, List<OrderLine>> linesByProduct = new HashMap<String, List<OrderLine>>();

// after
var linesByProduct = new HashMap<String, List<OrderLine>>();
```

Nothing was lost. The type is still right there on the same line, three words to the right.

The same applies to any right-hand side that names its own type — `new Something()`, a cast, a literal:

```java
var reader = new BufferedReader(new FileReader("orders.csv"));   // obvious
var scale  = new BigDecimal("19.99");                            // obvious
var i      = 0;                                                  // obvious
```

### Where `var` hurts

When the type is *not* on the line at all:

```java
var result = service.process(order);   // what is `result`?
```

The reader now has to open `process()` to find out what they are holding. That is a real cost, paid by every future reader of this line, to save you fourteen keystrokes once. In a code review this is the version you should push back on.

The awkward middle case is a method whose name *nearly* tells you:

```java
var orders = repository.findAllByStatus(ACTIVE);
```

A `List<Order>`? A `Set`? A `Stream` you have to close? A `Page<Order>` with pagination metadata on it? All four are plausible, and which one it is changes what you can do on the next line.

### The rule

> Use `var` when the right-hand side **already states the type**. Refuse it when the type comes from a method call whose return type you cannot guess from its name.

### The hard limits

`var` works **only** for local variables with an initializer. Not fields, not method parameters, not return types, and not without a value to infer from:

```java
public class Bad {
    var count = 0;                      // no - not on a field
    public var compute(var x) { ... }   // no - not on a parameter or a return type

    void method() {
        var nothing;                    // no - nothing to infer from
        var empty = null;               // no - null has no type of its own
    }
}
```

Four different error messages, one reason: `var` is the compiler copying a type it can already see. Where there is no type to see, there is nothing to copy.

### The gotcha worth knowing before you meet it

The diamond operator `<>` takes its type argument from the **left-hand side**. `var` removes the left-hand side. Put the two together and you get something that compiles, runs, and quietly does the wrong thing:

```java
List<String> a = new ArrayList<>();   // an ArrayList<String>
var          b = new ArrayList<>();   // an ArrayList<Object>

b.add("hello");
b.add(42);       // compiles. of course it does - it's an ArrayList<Object>
b.add(true);     // so does this
```

You did not get a warning, and you will not find out until something downstream casts. The fix is to spell the type argument out on the right, where `var` can see it:

```java
var b = new ArrayList<String>();
```

### Demo — `VarLab.java`

Your instructor runs this on screen:

```
java VarLab.java
```

Three variables with their types written out, then the same three with `var` — and the printed types are identical, because they are. Then one gotcha: `var` with a bare `<>`.

The file is in `examples/` if you want to replay it later. You do not need to run it during the session — your code today is in `src/`.

> **Pause.** Which of these would you write with `var`, and which not? Give a reason for each, and notice that two of them are genuinely arguable.
> 1. `var reader = new BufferedReader(new FileReader("data.csv"));`
> 2. `var config = loadConfiguration();`
> 3. `var i = 0;`
> 4. `var entries = repository.findAllByStatus(ACTIVE);`
> 5. `var total = order.total();`

---

## The data-class problem

Here is the `Order` class from the previous session:

```java
public class Order {
    private final String customerEmail;
    private final double total;

    public Order(String customerEmail, double total) {
        this.customerEmail = customerEmail;
        this.total = total;
    }

    public String customerEmail() { return customerEmail; }
    public double total() { return total; }
}
```

Twelve lines to say **"an order is an email and a total."**

And it is not even finished. It has no `equals`, so two orders with identical data are not equal. It has no `hashCode`, so you cannot put it in a `HashSet` and find it again. It has no `toString`, so it appears in your logs as `Order@342c38f8`. Write those three properly and the file is about forty lines, of which **thirty-eight say nothing a human needs to read**.

Now count how many classes like that a real system has. Every REST request body. Every response. Every row read from a database. Every message on a queue. Every value object in between. A medium-sized Spring service has dozens, and each one is forty lines of code that a tool generated and a human has to maintain.

There is a name for what goes wrong. It is not that the code is long — long is survivable. It is that **the boilerplate can go out of date**. Someone adds a third field to `Order` and forgets to add it to `equals`. Now two different orders compare as equal, a `HashSet` silently drops one of them, and a test passes that should have failed. Nothing in the language notices.

So the goal is to say the same thing in far less code **and** get more compiler help while doing it. The feature that does it is the **record**.

> **Pause.** Before reading on: from memory, write out a correct `equals` and `hashCode` for the `Order` class above. Do not use the IDE generator. Then check the two rules you had to remember — what must `equals` do when handed `null`, and what is the contract between `equals` and `hashCode`? Most people get one of the two wrong. That is the argument for records in one exercise.

---

## Records

### What a data class actually needs

A class whose only job is to carry data needs six things, and five of them are mechanical:

1. private final fields — **meaningful**, this is the actual content
2. a constructor assigning them — mechanical
3. an accessor per field — mechanical
4. `equals` comparing all fields — mechanical
5. `hashCode` consistent with `equals` — mechanical
6. `toString` listing the fields — mechanical

Your IDE will generate 2–6 for you. **That is the tell: if a tool can write it, the language should own it.** A generated method is not free — it is a copy of a fact, and copies drift.

### The whole `Order` class, as a record

```java
public record Order(String customerEmail, double total) { }
```

One line. And it is *more* complete than the twelve-line version, because it also has `equals`, `hashCode` and `toString`.

What the compiler generated for you:

| You get | Behaviour |
|---|---|
| `private final String customerEmail;` | Fields, always `private final` |
| `Order(String, double)` | The **canonical constructor** |
| `customerEmail()`, `total()` | Accessors — note: **no `get` prefix** |
| `equals(Object)` | `true` when *every* component is equal |
| `hashCode()` | Consistent with `equals` |
| `toString()` | `Order[customerEmail=alice@example.com, total=250.0]` |

Two details in that table catch people:

**The accessors have no `get`.** It is `order.total()`, not `order.getTotal()`. A record component is not a JavaBean property; the accessor is named after the thing itself. Most of the time this does not matter, but some older libraries look for `getXxx` by reflection and will not find it. Jackson — the JSON library Spring uses — understands records natively, so you will not hit this in this course.

**The class is `final`.** Not "you should not extend it" — you cannot. Nor can a record extend a class. It already extends `java.lang.Record`, and Java has single inheritance.

### Value semantics — the real reason to use a record

```java
var a = new Order("alice@example.com", 250.00);
var b = new Order("alice@example.com", 250.00);

a == b          // false - two separate objects. correct.
a.equals(b)     // true  - same data, so the SAME ORDER
```

Two orders with the same data **are** the same order. That is what "value semantics" means, and it is the property most of the rest of your code quietly depends on:

```java
var seen = new HashSet<Order>();
seen.add(a);
seen.contains(b);      // true with a record. false with the hand-written class.
```

Every `HashSet`, every `HashMap` key, every `list.contains(...)`, every `assertEquals` in a test goes through `equals` and `hashCode`. Get them wrong and you get bugs that do not look like `equals` bugs at all: a set that accepts duplicates, a cache that never hits, a test that fails while printing two identical-looking objects.

The hand-written class from the previous session gets all of that **wrong** — not because anyone made a mistake, but because nobody wrote the two methods. A record cannot get it wrong.

### What a record gives up

Records are **immutable** and **final**. You cannot:

- change a component after construction — there are no setters, and there is no way to add one
- extend a record, or have a record extend a class

That is the trade. In exchange for one line, you accept that the object never changes.

For data crossing a boundary — a request body, a response, a query result, a message — that is exactly what you want. An object that cannot change also cannot be corrupted by two threads at once, which is why you will see records everywhere in code that runs inside a server.

> **Pause.** `Order` is now immutable. A colleague asks: *"what if I need to apply a discount to an order?"* What do you tell them? (Hint: you do not modify it.)

The answer: you make a new one.

```java
public record Order(String customerEmail, double total) {

    public Order withDiscount(double percent) {
        return new Order(customerEmail, total * (1 - percent / 100));
    }
}
```

Yes — records can have methods. They just cannot have mutable state. A method like this is called a **wither**, by convention named `withXxx`, and it is how you "change" an immutable object: you do not, you produce a new one and leave the original alone.

### Compact constructors — validating on the way in

You usually want to reject bad data at construction. A **compact constructor** lets you do that without writing the whole constructor out:

```java
public record Order(String customerEmail, double total) {

    // Compact constructor - note: no parameter list, no field assignment
    public Order {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail is required");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
    }
}
```

Read the shape carefully, because it is unlike anything else in Java:

- **No parameter list.** `public Order {`, not `public Order(String customerEmail, double total) {`. The parameters are implied — they are the components.
- **No `this.customerEmail = customerEmail;`.** The compiler adds the assignments *after* your code runs.

That second point is what makes normalising possible. Reassigning the parameter changes what gets stored:

```java
public Order {
    if (customerEmail == null || customerEmail.isBlank()) {
        throw new IllegalArgumentException("customerEmail is required");
    }
    customerEmail = customerEmail.trim().toLowerCase();   // normalised before assignment
}
```

This is the one place in Java where reassigning a parameter is a feature rather than a code smell.

And the payoff is worth being precise about: **an invalid `Order` can now never exist.** Not "is checked in the service layer", not "is usually validated" — cannot exist. There is no sequence of legal Java that produces an `Order` with a negative total. Every method downstream is freed from re-checking, and every one of those `if (order.total() < 0)` guards you would have written is now dead code you never wrote.

> **Pause.** Compare two designs: (a) an `Order` record with a compact constructor rejecting negative totals; (b) a mutable `Order` class with a `validate()` method you call after construction. In which design can an invalid `Order` exist? For how long? And who has to remember to call `validate()`?

### The trap: records are only *shallowly* immutable

This is the thing that catches everyone once, and it is worth catching now rather than in production.

```java
public record Basket(String owner, List<String> items) { }

var items = new ArrayList<String>();
items.add("widget");

var basket = new Basket("alice", items);

items.add("SNEAKED IN");             // the caller still holds the list
basket.items().add("AND ANOTHER");   // and so does anyone we hand it to

basket    // Basket[owner=alice, items=[widget, SNEAKED IN, AND ANOTHER]]
```

The **field** is `final`. The **list** is not. `final` means the reference cannot be re-pointed at a different object; it says nothing whatsoever about the object it points at.

So: **a record is only as immutable as its components.** With `String`, `int`, `BigDecimal`, `LocalDate` or another record, you are fine — those are immutable themselves. With a `List`, `Map`, array or any mutable class, you are not.

The fix is one line in a compact constructor:

```java
public record Basket(String owner, List<String> items) {
    public Basket {
        items = List.copyOf(items);
    }
}
```

`List.copyOf` does two jobs at once. It **copies**, so the caller's later edits cannot reach in; and the copy is **unmodifiable**, so anyone who gets the list back from `items()` cannot edit it either. (It also rejects a null list, so decide deliberately whether you want `items == null ? List.of() : List.copyOf(items)` instead.)

### Where a record is the wrong answer

Records are not a replacement for classes. They are a replacement for one *kind* of class. Do not reach for one when:

| Situation | Why not |
|---|---|
| The object needs to change after construction | No setters, ever. This is not negotiable. |
| It has behaviour, not data — a service, a repository, a gateway | Those are not data carriers. `FakePaymentGateway` in today's exercise is a class, correctly. |
| It needs to extend a class | Records already extend `Record`. |
| It is a JPA entity | See below. This one is worth its own paragraph. |

**Records for DTOs, classes for JPA entities.** A JPA entity has to be mutable (Hibernate sets fields on the object after it loads it) and needs a no-argument constructor (Hibernate calls it before it has any data). A record has neither and cannot be given either. This trips up almost everyone the first time; the persistence course covers it properly. For now, just carry the sentence.

### Demo — `RecordAnatomy.java`

```
java RecordAnatomy.java
```

The hand-written class prints three lines — `OldOrder@76ed1b7c`, `equals` false, and a `HashSet` that cannot find what it holds. The record prints the same three lines, correct. Then two more short sections: the compact constructor, and the trap.

The file is in `examples/` if you want to replay it later.

> **Pause.** Find a data-carrier class in code you have written — anything that is only fields, a constructor and getters. Two questions. First: how many lines is it, and how many would survive as a record? Second, and more interesting: **does it have a setter that is actually called anywhere?** If yes, it is not a data carrier, and you should write down what it really is.

---

## Sealed classes and interfaces

### The problem

Charging an order can end three ways: it is approved, the payment is declined, or the product is out of stock. How do you return that?

Most codebases pick one of these three, and all three are bad:

```java
// (a) a boolean - where does the decline reason go?
boolean placeOrder(Order order);

// (b) exceptions as control flow - expensive, and the caller can simply not catch
void placeOrder(Order order) throws PaymentDeclinedException, OutOfStockException;

// (c) a status blob - nothing stops you building an invalid combination
record Result(boolean ok, String errorCode, String message, Integer available) {}
```

Option (c) is the interesting failure, because it *looks* fine. It compiles, it is a record, it holds everything you need. Here is what it also allows:

```java
new Result(true,  "DECLINED", "Amount exceeds limit", null);   // succeeded AND declined
new Result(false, null,       null,                   null);   // failed, won't say why
new Result(true,  null,       null,                   0);      // succeeded, reports stock
```

Three states that mean nothing, all constructed with ordinary legal Java. Somewhere in your codebase there is now a comment saying *"if `ok` is true, `errorCode` will be null"* — which is documentation, not a rule. Nothing enforces it. Someone will break it on a Thursday.

What you actually want is: **there are exactly three outcomes, each carries its own data, and the compiler knows the list is complete.**

### Sealed interfaces

`sealed` means *"these are the only permitted implementations."*

```java
public sealed interface PaymentResult
        permits Approved, Declined, OutOfStock { }

public record Approved(String transactionId)        implements PaymentResult { }
public record Declined(String reason)               implements PaymentResult { }
public record OutOfStock(String sku, int available) implements PaymentResult { }
```

Read the first two lines as an English sentence: *a `PaymentResult` is an `Approved`, a `Declined`, or an `OutOfStock` — and nothing else, ever.*

Three things follow.

**1. Each outcome carries exactly the data it needs.** `Approved` has a transaction id. `Declined` has a reason. `OutOfStock` has a SKU and a quantity. There is no field that is meaningless half the time, and no combination that needs a comment to explain when it applies. Look at the three declarations again: not one of the nonsense states above can be written.

**2. Nobody can add a fourth outcome.** Not from another package, not from another module, not from a library that depends on yours. The set is closed at compile time, and the compiler enforces it:

```java
record Refunded(String transactionId) implements PaymentResult { }
// error: class is not allowed to extend sealed class: PaymentResult
//        (as it is not listed in its 'permits' clause)
```

That error is the whole feature in one message. `Refunded` is a perfectly reasonable record written by a perfectly reasonable developer, and the compiler refuses it because **the author of the interface did not list it**. Compare that with what happens when someone adds a fifth value to a `String errorCode` field: nothing. It compiles, it deploys, and a customer tells you.

**3. The compiler knows the list is complete.** That is the payoff, and it arrives next session in the form of an exhaustive `switch`:

```java
// NEXT SESSION - shown here so you know what today is buying
String describe(PaymentResult result) {
    return switch (result) {
        case Approved a   -> "Approved: " + a.transactionId();
        case Declined d   -> "Declined: " + d.reason();
        case OutOfStock o -> "Only " + o.available() + " left of " + o.sku();
        // no `default` needed - the compiler knows there are exactly three
    };
}
```

Add a fourth outcome to the `permits` list and **every switch that does not handle it stops compiling**. The compiler walks you to each place that needs updating and names it. Compare that with adding a new value to a status `String`, where nothing breaks and you find out in production.

### The three rules for permitted subtypes

A permitted subtype must do two things, and there is a third rule about where it lives:

1. It must **actually implement or extend** the sealed type. Listing it in `permits` is not enough.
2. It must declare itself `final`, `sealed`, or `non-sealed`. Exactly one of the three — the compiler will not let you leave it open by accident.
3. It must be in the **same module**, or, if you are not using modules, the **same package**. In practice this means the sealed type and its permitted subtypes are written and reviewed together, which is the point.

Records satisfy rule 2 for free: a record is implicitly `final`. That is why the three above needed no extra keyword, and it is the main reason "sealed interface over records" is the idiom you will see.

If the permitted types are in the same file, you may even leave `permits` off entirely and the compiler infers it — but write it out anyway. It is the documentation.

### `non-sealed` — the escape hatch, and why to be suspicious of it

```java
public sealed interface PaymentResult permits Approved, Declined, PartialResult { }

public non-sealed class PartialResult implements PaymentResult { }   // anyone may extend this
```

`non-sealed` re-opens one branch: the hierarchy is closed at the top and open underneath. It exists for real cases — usually a framework offering a small closed set plus one extension point.

Ask the question before you use it, though: *what did the seal buy me if one branch is still open?* Exhaustiveness still works (a `switch` over `PaymentResult` still only needs three cases, because any subclass of `PartialResult` **is** a `PartialResult`), but the "each outcome carries exactly its own data" property is gone for that branch. Sometimes worth it. Usually a smell.

### Sealed classes work the same way

```java
public sealed class Shape permits Circle, Square { }

public final class Circle extends Shape { }
public final class Square extends Shape { }
```

Same rules, same benefits. In practice you will reach for a sealed **interface** far more often, because the permitted types are usually records and a record cannot extend a class.

### Sealed types vs. enums

The obvious question: an enum is also a closed set. When is a sealed interface the better answer?

| | `enum` | sealed interface + records |
|---|---|---|
| The set is closed | ✅ | ✅ |
| Each case carries **its own** data | ❌ every constant has the same fields | ✅ different components per case |
| There is one instance per case | ✅ a singleton per constant | ❌ as many as you construct |
| Exhaustive `switch` | ✅ | ✅ (when sealed) |

The dividing line is data. `Status { PENDING, SHIPPED, DELIVERED }` is an enum — the cases carry nothing but their own identity. `Approved(transactionId)` / `Declined(reason)` is a sealed interface, because each case carries something different. If you find yourself writing an enum with three fields, two of which are null for most constants, you wanted a sealed interface.

### When to seal, and when not to

| Seal it | Leave it open |
|---|---|
| A fixed set of outcomes, states, or events | A plugin or strategy interface meant to be extended |
| You want an exhaustive `switch` | `OrderRepository` from the previous session — new implementations are the whole point |
| The set is a **domain fact**: "a payment is approved, declined, or out of stock" | Anything a third party should be able to implement |
| You own every implementation, and want to keep it that way | Anything where you want to *not* know the implementations |

The distinction is about who is allowed to extend your design. A sealed type says *"this list is a fact about the domain and I will change it deliberately."* An open interface says *"surprise me."* Both are correct, for different jobs.

> **Pause.** Should `OrderRepository` from the previous session be sealed? Answer with a reason, not a feeling. Then ask the same question about `PaymentGateway` in today's exercise, and notice that the answer is the same but the reason is slightly different.

### What sealing does **not** buy you today

Be honest with yourself about this, because it is the thing that makes next session land.

Today you will branch on a sealed type using `instanceof` with a pattern variable:

```java
if (result instanceof Approved a) {
    return "Approved: " + a.transactionId();
}
if (result instanceof Declined d) {
    return "Declined: " + d.reason();
}
if (result instanceof OutOfStock o) {
    return "Only " + o.available() + " left of " + o.sku();
}
throw new IllegalStateException("unknown outcome: " + result);   // unreachable
```

Note two things. There is **no cast** — `a` is already an `Approved`; the pattern variable is the modern replacement for `instanceof` followed by a cast on the next line, and it is worth the change on its own.

But that final `throw` is dead code. You know it can never run: the type is sealed, and all three permitted outcomes are handled above. The compiler does not know it yet, and it will not let a method end without returning.

Worse: add a fourth outcome to `permits` and **this method still compiles**. Nothing warns you. It just throws at runtime for the new case.

Hold on to that annoyance. It is exactly what an exhaustive `switch` fixes next session — and sealing *today* is what makes that possible.

### Demo — `SealedProof.java`

```
java SealedProof.java
```

Two status-blob states that compile and mean nothing, then the three sealed records that cannot express either of them.

**The moment to watch for** is the compile error your instructor triggers live — a fourth outcome added without permission, producing the message quoted above. Reading it on screen is worth more than reading it here.

The file is in `examples/` if you want to replay it later.

> **Pause.** Look again at option (c), the status blob: `boolean ok, String errorCode, String message, Integer available`. Write down **two** field combinations that are nonsensical but that the type system happily allows. Now check: can either of them be constructed out of `Approved`, `Declined` and `OutOfStock`?

---

## Text blocks

A multi-line string in Java 8:

```java
String sql = "SELECT p.id, p.name, p.price " +
             "FROM product p " +
             "JOIN category c ON c.id = p.category_id " +
             "WHERE c.name = ? AND p.active = true " +
             "ORDER BY p.price DESC";
```

Count the ways that goes wrong. Every line needs a trailing space *inside* the quotes or the SQL runs together — miss one and you get `FROM product pJOIN category c`, which fails at runtime with a message about syntax near `pJOIN`. Every line needs a `+`. And nobody reviewing it can see the SQL for the punctuation.

The same thing as a **text block** (Java 15+):

```java
String sql = """
        SELECT p.id, p.name, p.price
        FROM product p
        JOIN category c ON c.id = p.category_id
        WHERE c.name = ? AND p.active = true
        ORDER BY p.price DESC
        """;
```

No concatenation, no trailing spaces to get wrong, and the SQL looks like SQL.

JSON is where it really pays, because you stop escaping quotes:

```java
// before - every quote escaped
String json = "{\"name\":\"Widget\",\"price\":9.99,\"tags\":[\"new\",\"sale\"]}";

// after
String json = """
        {
          "name": "Widget",
          "price": 9.99,
          "tags": ["new", "sale"]
        }
        """;
```

You will write text blocks constantly from here on: JPQL queries in the persistence course, expected JSON in controller tests once you reach Spring MVC.

**A text block is not a new type.** It produces an ordinary `String`. Anything that takes a `String` takes one, `equals` works normally against a concatenated string, and there is no runtime cost — the compiler does all the work.

### Rule 1 — incidental indentation is stripped

The compiler finds the **least-indented non-blank line** and removes that much whitespace from every line. The closing `"""` counts as one of those lines.

That last clause is the whole rule in practice: **where you put the closing delimiter decides the answer.**

```java
String a = """
        hello
        """;        // "hello\n"   - closing """ aligned with content

String b = """
        hello
""";                // "        hello\n"  - closing """ at column 0 keeps the indent

String c = """
        hello""";   // "hello"     - no line break after hello, so no trailing \n
```

`a` and `c` differ by exactly one character. `b` has eight leading spaces that you typed to make the *source* readable and never wanted in the string.

**The habit:** keep the closing `"""` in the same column as your content, unless you deliberately want leading spaces.

### Rule 2 — a trailing newline is included

If the closing `"""` is on its own line, the last content line ends with a `\n`. That is usually what you want for a receipt or a log line, and usually *not* what you want for a query or a piece of JSON you are about to compare in a test.

Two ways to suppress it:

```java
// (i) closing delimiter on the content line
String c = """
        hello""";

// (ii) a backslash at the end of the last line - it swallows the line break
String d = """
        hello \
        world""";      // "hello world" - no newline anywhere
```

Option (ii) is also how you keep one very long line readable in source without putting a `\n` in the value.

### The one escape you will eventually need

Text blocks **strip trailing whitespace from every line**, always, silently. Usually a mercy. Occasionally the exact thing you wanted:

```java
String e = """
        hello\s\s\s
        world""";      // "hello   \nworld" - the three spaces survive
```

`\s` is an escape for a single space that survives the stripping. You need it rarely — but when a test compares two strings character by character and they look identical in the failure message, this is often what is missing.

### Filling in the holes

There is **no string interpolation in Java**. Writing `${name}` inside a text block gives you the literal characters `$`, `{`, `n`, `a`, `m`, `e`, `}`. Use `.formatted(...)`, which is `String.format` with the format string on the left and reads much better with a text block:

```java
String receipt = """
        RECEIPT
        -------
        Customer: %s
        Total:    $%.2f
        Status:   %s
        """.formatted(customer, total, status);
```

### When *not* to use one

A text block earns its keep from about three lines up. Below that it is ceremony:

```java
String name = """
        Widget""";      // four lines of machinery for one word
String name = "Widget"; // do this
```

### Demo — `TextBlockLab.java`

```
java TextBlockLab.java
```

SQL glued with `+`, printing `FROM product pJOIN category c`, and JSON you cannot read through the backslashes. Then the same two strings as text blocks. It ends on the one rule that will cost you time in the exercise: where the closing `"""` sits, shown as `[hello\n]` against `[hello]`.

The file is in `examples/` if you want to replay it later.

> **Pause.** Predict the exact value of `a`, `b` and `c` above — every space, every `\n` — before you run anything. Then check with `System.out.println("[" + a + "]")`. Expect to get one wrong. The one you get wrong is the one that would have cost you twenty minutes in a failing test.

---

## Where all four show up in Spring

None of this is academic. Here is where you will meet each feature again in this course and the ones after it:

| Feature | Where |
|---|---|
| **Records** | Request and response DTOs — `record CreateProductRequest(String name, BigDecimal price)`, with Bean Validation annotations on the components. Jackson serialises and deserialises them natively. |
| **Records** | Query projections in the persistence course: `select new com.example.ProductSummary(p.name, p.price) from Product p`. |
| **Records** | *Not* JPA entities. Entities must be mutable and need a no-arg constructor. |
| **Sealed types** | Modelling the result of an operation that can fail in more than one way, so a controller can map each case to a different HTTP status without reading a string. |
| **`var`** | Everywhere in test setup and in local wiring, where the right-hand side is a `new`. |
| **Text blocks** | JPQL and native queries; expected JSON in `MockMvc` tests; multi-line configuration values. |

And one connection worth making now: the previous session's argument was that **a good type stops a change from spreading**. Everything today is the same argument at a smaller scale. A record stops `equals` from drifting out of date. A compact constructor stops an invalid object from existing. A sealed interface stops a fourth outcome from appearing without anyone noticing. In each case the compiler is doing work that would otherwise be a code review's job.

---

## Exercise — Modernise the order domain

Open `src/`. You are refactoring the previous session's domain to modern Java.

**Given, complete and working — read, do not change:** `PaymentGateway.java` (the interface), `Main.java`, `Verify.java`.

Everything else has `TODO:` comments telling you exactly what to write.

### How you check your work

From a terminal in `src/`:

```
javac *.java
java Verify
```

Both commands, after every edit — up-arrow twice to repeat them. If your shell does not expand `*.java`, name the ten files explicitly.

It runs **24 checks**. Some are *behavioural* — they call your code and compare the answer. Others are *structural* — they ask the JVM questions about your types: is `Order` actually a record? Is `PaymentResult` actually sealed? Are all three outcomes records? You need both. Code that behaves correctly but is not built from the right types has missed the point of the session, and the structural checks are there to say so.

**Expect the unedited folder not to compile.** You get **nine errors** from exactly two causes: `Order` has no `withDiscount` yet, and the three outcome types are still empty classes with no constructors. That is normal, and it is why Steps 1 and 2 come first. Do them both, then compile.

### Step 1 — `Order` as a record (~10 min)

Replace `Order.java` with a record having components `customerEmail` (String) and `total` (double).

Add a **compact constructor** that:

- rejects a null or blank `customerEmail` with `IllegalArgumentException("customerEmail is required")`
- rejects a negative `total` with `IllegalArgumentException("total cannot be negative")`
- normalises the email: trimmed and lower-cased

Add one method: `withDiscount(double percent)` returning a **new** `Order` with the total reduced by that percentage.

When you are done the whole file should be about twenty lines including comments.

### Step 2 — `PaymentResult` as a sealed interface (~15 min)

Four files move together here, so do all four before you compile.

Fill in `PaymentResult.java` as a sealed interface permitting exactly `Approved`, `Declined` and `OutOfStock`, then make each of those three a record:

- `Approved(String transactionId)`
- `Declined(String reason)`
- `OutOfStock(String sku, int available)`

Then implement `PaymentGateway.charge(Order order)` in `FakePaymentGateway.java` using these rules, **checked in this order**:

1. total is over `1000.00` → `Declined("Amount exceeds limit")`
2. the email starts with `"nostock"` → `OutOfStock("SKU-001", 0)`
3. otherwise → `Approved("TX-1001")` — a real gateway would generate an id; a fixed one keeps the output stable

The order matters and `Verify` checks it: a customer who is both over the limit **and** a `nostock` address must come back `Declined`, because rule 1 is tested first.

### Step 3 — describe the result (~10 min)

In `OrderReporter.describe(PaymentResult result)`, return:

| Outcome | Returned string |
|---|---|
| `Approved` | `Approved: TX-1001` |
| `Declined` | `Declined: Amount exceeds limit` |
| `OutOfStock` | `Only 0 left of SKU-001` |

Use `instanceof` with pattern variables — note there is no cast:

```java
if (result instanceof Approved a) {
    return "Approved: " + a.transactionId();
}
```

You will need a `throw` at the end or the method will not compile. Write it, then notice how annoying it is. That is the point.

### Step 4 — a text block receipt (~10 min)

In `OrderReporter.receipt(Order order, PaymentResult result)`, build this with a **text block** — no `+` across lines:

```
RECEIPT
-------
Customer: alice@example.com
Total:    $250.00
Status:   Approved: TX-1001
```

It **ends with a newline**. Use `.formatted(...)` and watch the indentation rule.

Call `describe(result)` for the last line rather than hard-coding it — `Verify` checks this template against two different outcomes.

### Step 5 — the same data as JSON (~10 min)

In `OrderReporter.summaryJson(Order order, PaymentResult result)`, produce exactly:

```
{
  "customer": "alice@example.com",
  "total": 250.00,
  "status": "Approved: TX-1001"
}
```

Two things make this different from Step 4:

1. It is full of double quotes and **not one of them is escaped**. Compare it with what you would have written in Java 8.
2. It must **not** end in a newline. There are two ways to do that; `examples/TextBlockLab.java` shows both. Pick one and be able to say why.

Use `%.2f` for the total so it prints `250.00`, not `250.0`.

### Step 6 — run it (~5 min)

`Main.java` places three orders and then proves two things about records. Expected output:

```
RECEIPT
-------
Customer: alice@example.com
Total:    $250.00
Status:   Approved: TX-1001

RECEIPT
-------
Customer: rich@example.com
Total:    $5000.00
Status:   Declined: Amount exceeds limit

RECEIPT
-------
Customer: nostock@example.com
Total:    $50.00
Status:   Only 0 left of SKU-001

--- the same first order, as JSON ---
{
  "customer": "alice@example.com",
  "total": 250.00,
  "status": "Approved: TX-1001"
}

Discounted order: Order[customerEmail=alice@example.com, total=225.0]
The original:     Order[customerEmail=alice@example.com, total=250.0]
Same value? true
```

Two things to actually look at rather than skim past.

The first order is constructed as `new Order("Alice@Example.com ", 250.00)` — mixed case, trailing space — and comes out as `alice@example.com`. **Your compact constructor did that**, once, at the boundary, and nothing downstream had to know.

And that last line, `Same value? true`, is the record's value semantics: two separately-constructed orders comparing equal. The previous session's class printed `false` here. Confirm it.

### Step 7 — run the checks (~5 min)

```
javac *.java
java Verify
```

Work the failures from the top; the later steps cannot pass until the earlier ones do.

`ALL 24 CHECKS PASSED` means you are finished.

### If you get stuck

| The compiler or `Verify` says | What it means | Fix |
|---|---|---|
| `cannot find symbol: method withDiscount(int)` | Step 1 not done. Nothing compiles until it is. | Add `withDiscount` to the record. |
| `constructor Approved in class Approved cannot be applied to given types` | Step 2: `Approved` is still an empty class, not a record. | `public record Approved(String transactionId) implements PaymentResult { }` |
| `class is not allowed to extend sealed class: PaymentResult` | You sealed the interface but left a type out of `permits` — or a name is misspelled. | All three names, spelled exactly, in the `permits` clause. |
| `... might not have been initialized` inside the compact constructor | You wrote `this.customerEmail = ...`. A compact constructor does not assign fields. | Assign the **parameter**: `customerEmail = customerEmail.trim();` |
| `missing return statement` in `describe` | Not every path returns. | Add the final `throw new IllegalStateException(...)`. |
| `it permits exactly three outcomes` fails | `sealed` without a `permits` clause, or a type listed twice. | Three names, once each. |
| A text-block check fails and the two strings look identical | Whitespace. `Verify` will say so explicitly when that is the only difference. | Count the leading spaces — your closing `"""` is in the wrong column. Then count the `\n` at the end. |
| `the JSON matches exactly, and does NOT end in a newline` fails, and everything looks right | One trailing `\n`. | Move the closing `"""` onto the `}` line, or end that line with `\`. |
| `the rules are applied in order` fails | Your gateway checks the email before the limit. | Limit first, then `nostock`, then approve. |

### Finished early? Three extensions

Optional, and deliberately **not** checked by `Verify` — the count stays at 24. Verify them by reading the output.

| | Extension | The point |
|---|---|---|
| **A** | Add `record Refunded(String transactionId) implements PaymentResult { }` **without** touching `permits`, and compile | Read the error out loud. That is the seal doing its job. |
| **B** | Now add it to `permits` properly, and route an order over `10000.00` to it | `describe()` **still compiles** — and throws at runtime. Nothing warned you. Write down what you wanted the compiler to do. That is next session's `switch`. |
| **C** | Give `Order` a `List<String> tags` component and try to edit it from `Main` | The shallow-immutability trap, on your own code. Fix it with `List.copyOf` in the compact constructor. |

Extension B is the one worth doing even if you only do one. It creates the itch that next session scratches.

---

## Homework — the support desk

`homework/` is a second, self-contained exercise in a different domain: a support ticket desk. Same four features, and one thing today's exercise never tested.

Read `homework/README.md`, then work in that folder:

```
javac *.java
java Verify
```

**26 checks.** Budget 45–60 minutes.

The part that is new: `Ticket` holds a `List<String> tags`, and two checks try to reach inside your record from the outside. `final` will not save you — this is the shallow-immutability trap from `examples/RecordAnatomy.java`, and closing it is one line.

Bring it to the next session. The `describe()` method you write there is the one we rewrite as a `switch`.

---

## Recap

- **`var`** infers a local variable's type. Use it when the right-hand side already states the type; refuse it when the type comes from a method call you would have to go and read. Locals with an initializer only — never fields, parameters or return types.
- **`var` plus a bare diamond gives you `Object`.** Spell the type argument out on the right.
- **Records** are one-line immutable data carriers. You get `private final` fields, a canonical constructor, accessors with **no `get` prefix**, `equals`, `hashCode` and `toString`. They are final and immutable — that is the trade.
- **Value semantics** are the reason to use one: two records with the same components are equal, so `HashSet`, `HashMap` keys and `assertEquals` all behave.
- **Compact constructors** validate and normalise before assignment, which means an invalid instance cannot exist — not "is usually caught", cannot exist.
- **A record is only as immutable as its components.** A `List` component is a hole; `List.copyOf` in the compact constructor closes it.
- **Records for DTOs, classes for JPA entities.** Entities need mutability and a no-arg constructor.
- **Sealed** types close a hierarchy: `permits` lists every implementation, so each outcome carries only its own data and no nonsense combination can be constructed.
- A permitted subtype must be `final`, `sealed` or `non-sealed`, and live in the same package or module. Records are implicitly final, which is why sealed-interface-over-records is the idiom.
- **Enum when the cases carry nothing; sealed interface when each case carries something different.**
- Sealing unlocks **exhaustive `switch`** next session. Today it buys you a closed set and a compile error for gatecrashers — not yet exhaustiveness.
- **`instanceof` with a pattern variable** replaces `instanceof` plus a cast. No casts.
- **Text blocks** remove concatenation and escaping. Two rules: where the closing `"""` sits decides the indentation, and whether it is on its own line decides the trailing newline.
- There is **no string interpolation in Java**. Use `.formatted(...)`.

## Glossary

| Term | Meaning |
|---|---|
| Type inference | The compiler deducing a type you did not write; the type is still static and fixed |
| Record | A final, immutable class whose state is its component list |
| Component | A field declared in a record's header |
| Canonical constructor | The constructor whose parameters are exactly the record's components |
| Compact constructor | A shorthand canonical constructor with no parameter list; the compiler assigns the fields after your code runs |
| Wither | A method returning a **new** instance with one thing changed, by convention named `withXxx` |
| Value semantics | Two instances with equal components are `equals` |
| Shallow immutability | The fields cannot be re-pointed, but the objects they point at may still be mutable |
| Defensive copy | Copying a mutable argument on the way in so the caller cannot reach inside afterwards |
| Sealed type | A class or interface that lists its permitted subtypes with `permits` |
| `non-sealed` | A permitted subtype that re-opens its own branch of the hierarchy |
| Exhaustiveness | The compiler's ability to prove a `switch` covers every possible case |
| Pattern variable | The variable bound by `instanceof Approved a` — already the right type, no cast |
| Text block | A `"""`-delimited multi-line string literal |
| Incidental indentation | Leading whitespace a text block strips, determined by the least-indented line, closing delimiter included |

## Take it home

Beyond the graded homework, three things to do in **your own** code:

1. Find a data-carrier class — anything with only fields, getters and a constructor. Convert it to a record. Count the lines you deleted.
2. Did that class have a setter that was actually called anywhere? If yes, it is not a data carrier — write down what it really is.
3. Find one method returning a `boolean` or a status `String` to signal *how* something ended. Redesign it as a sealed interface with one record per outcome. Then count the `if (result.getErrorCode().equals(...))` comparisons that disappear.

## Looking ahead

Next session takes the sealed `PaymentResult` you just built and replaces your `if (x instanceof ...)` chain with a single exhaustive `switch` expression — the dead `throw` at the end goes away, and adding a fourth outcome becomes a compile error instead of a runtime surprise. Then streams, and virtual threads.
